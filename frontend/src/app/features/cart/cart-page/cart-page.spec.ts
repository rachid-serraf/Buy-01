import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { CartDto, CheckoutResponse } from '../../../core/models/cart.model';
import { OrderAddress } from '../../../core/models/order.model';
import { CartService } from '../../../core/services/cart.service';
import { CartItem } from '../cart.mock';
import { cartItems } from '../cart-ui.state';
import { CartPage } from './cart-page';

describe('CartPage', () => {
  let fixture: ComponentFixture<CartPage>;
  let cartService: jasmine.SpyObj<CartService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const address: OrderAddress = {
    fullName: 'Ada Lovelace',
    phone: '+1 555 0100',
    line1: '1 Main St',
    city: 'Springfield',
    postalCode: '62701',
    country: 'USA',
  };

  const line = (overrides: Partial<CartItem> = {}): CartItem => ({
    id: 'a',
    name: 'Mug',
    category: 'Home',
    price: 38,
    quantity: 1,
    stock: 5,
    image: 'data:,',
    ...overrides,
  });

  const toCart = (items: CartItem[]): CartDto => ({
    id: 'cart-1',
    userId: 'user-1',
    items: items.map((item) => ({
      productId: item.id,
      sellerId: 'seller-1',
      name: item.name,
      unitPrice: item.price,
      imageId: null,
      quantity: item.quantity,
    })),
  });

  const el = () => fixture.nativeElement as HTMLElement;
  const text = (selector: string) => el().querySelector(selector)?.textContent?.trim();

  beforeEach(() => {
    cartItems.set([line()]);

    cartService = jasmine.createSpyObj<CartService>('CartService', [
      'getCart',
      'addItem',
      'checkout',
      'removeItem',
    ]);
    cartService.getCart.and.callFake(() => of(toCart(cartItems())));
    cartService.checkout.and.callFake(() =>
      of<CheckoutResponse>({ valid: true, issues: [], cart: toCart(cartItems()) }),
    );
    cartService.removeItem.and.callFake((productId: string) =>
      of(toCart(cartItems().filter((item) => item.id !== productId))),
    );

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({
      afterClosed: () => of(address),
    } as ReturnType<MatDialog['open']>);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: CartService, useValue: cartService },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(CartPage);
  });

  it('lists the items with the breadcrumb and summary', () => {
    cartItems.set([line(), line({ id: 'b', name: 'Lamp' })]);
    fixture.detectChanges();

    expect(el().querySelectorAll('app-cart-item').length).toBe(2);
    expect(text('.breadcrumb .current')).toBe('Cart');
    expect(text('.page-title')).toBe('Shopping cart');
    expect(text('.page-subtitle')).toBe('2 items');
  });

  it('loads the cart from the API', () => {
    cartService.getCart.and.returnValue(of(toCart([line({ id: 'remote', name: 'Remote' })])));

    fixture.detectChanges();

    expect(cartService.getCart).toHaveBeenCalled();
    expect(cartItems().map((item) => item.name)).toEqual(['Remote']);
  });

  it('uses the singular for one item', () => {
    fixture.detectChanges();
    expect(text('.page-subtitle')).toBe('1 item');
  });

  it('adds delivery to the total below the free threshold', () => {
    fixture.detectChanges();

    const rows = el().querySelectorAll('.summary-rows dd');
    expect(rows[0].textContent).toContain('$38.00');
    expect(rows[1].textContent).toContain('$4.99');
    expect(rows[1].classList).not.toContain('free');
    expect(text('.summary-total dd')).toBe('$42.99');
    expect(text('.bar-total')).toBe('$42.99');
  });

  it('shows free delivery above the threshold', () => {
    cartItems.set([line({ price: 60 })]);
    fixture.detectChanges();

    const delivery = el().querySelectorAll('.summary-rows dd')[1];
    expect(delivery.textContent).toContain('Free');
    expect(delivery.classList).toContain('free');
    expect(text('.summary-total dd')).toBe('$60.00');
  });

  it('shows the empty state when the cart has no items', () => {
    cartItems.set([]);
    fixture.detectChanges();

    expect(text('.empty-state h3')).toBe('Your cart is empty');
    expect(el().querySelector('.empty-state a')?.textContent).toContain('Continue shopping');
    expect(el().querySelector('.summary')).toBeNull();
    expect(el().querySelector('.buy-bar')).toBeNull();
  });

  it('switches to the empty state when the last item is removed', () => {
    fixture.detectChanges();

    (el().querySelector('.remove-link') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(el().querySelector('.empty-state')).not.toBeNull();
  });

  it('previews the empty state from the query parameter', () => {
    fixture.componentRef.setInput('state', 'empty');
    fixture.detectChanges();

    expect(el().querySelector('.empty-state')).not.toBeNull();
    expect(el().querySelector('app-cart-item')).toBeNull();
  });

  it('previews the loading state from the query parameter', () => {
    fixture.componentRef.setInput('state', 'loading');
    fixture.detectChanges();

    expect(el().querySelectorAll('.skeleton-row').length).toBe(3);
    expect(el().querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(el().querySelector('app-cart-item')).toBeNull();
  });

  it('shows no promo message for a blank code', () => {
    fixture.detectChanges();
    const input = el().querySelector('.promo input') as HTMLInputElement;

    input.value = '   ';
    el().querySelector('.promo')?.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();

    expect(text('.promo-message')).toBe('');
  });

  it('asks for a delivery address and checks out the cart', async () => {
    fixture.detectChanges();

    await fixture.componentInstance.checkout();
    fixture.detectChanges();

    expect(dialog.open).toHaveBeenCalled();
    expect(cartService.checkout).toHaveBeenCalledWith(address);
    expect(text('.alert-success')).toContain('Order placed successfully');
  });

  it('does not check out when the address form is cancelled', async () => {
    dialog.open.and.returnValue({
      afterClosed: () => of(undefined),
    } as ReturnType<MatDialog['open']>);
    fixture.detectChanges();

    await fixture.componentInstance.checkout();

    expect(cartService.checkout).not.toHaveBeenCalled();
  });

  it('shows the product changes returned by checkout', async () => {
    cartService.checkout.and.returnValue(
      of<CheckoutResponse>({
        valid: false,
        issues: [
          {
            productId: 'a',
            name: 'Mug',
            type: 'PRICE_CHANGED',
            message: 'Price changed from 38 to 42',
          },
        ],
        cart: toCart(cartItems()),
      }),
    );

    fixture.detectChanges();
    await fixture.componentInstance.checkout();
    fixture.detectChanges();

    expect(text('.alert-warning')).toContain('Price changed from 38 to 42');
    expect(el().querySelector('.alert-success')).toBeNull();
  });
});
