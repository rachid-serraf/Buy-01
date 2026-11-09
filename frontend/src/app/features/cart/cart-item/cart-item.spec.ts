import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CartDto } from '../../../core/models/cart.model';
import { CartService } from '../../../core/services/cart.service';
import { CartItem } from '../cart.mock';
import { cartItems } from '../cart-ui.state';
import { CartItemRow } from './cart-item';

describe('CartItemRow', () => {
  let fixture: ComponentFixture<CartItemRow>;
  let cartService: jasmine.SpyObj<CartService>;

  const line: CartItem = {
    id: 'a',
    name: 'Mug',
    category: 'Home',
    price: 18,
    quantity: 2,
    stock: 5,
    image: 'data:,',
  };

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

  function render(item: CartItem, compact = false): void {
    cartItems.set([item]);
    fixture.componentRef.setInput('item', item);
    fixture.componentRef.setInput('compact', compact);
    fixture.detectChanges();
  }

  beforeEach(() => {
    cartService = jasmine.createSpyObj<CartService>('CartService', ['removeItem']);
    cartService.removeItem.and.callFake((productId: string) =>
      of(toCart(cartItems().filter((item) => item.id !== productId))),
    );

    TestBed.configureTestingModule({
      providers: [{ provide: CartService, useValue: cartService }],
    });
    fixture = TestBed.createComponent(CartItemRow);
  });

  it('shows the full card on the cart page', () => {
    render(line);

    expect(el().querySelector('.cart-item.card')).not.toBeNull();
    expect(el().querySelector('.badge-accent')?.textContent).toContain('Home');
    expect(el().querySelector('.name')?.textContent).toContain('Mug');
    expect(el().querySelector('.unit-price')?.textContent).toContain('$18.00');
    expect(el().querySelector('.line-total')?.textContent).toContain('$36.00');
    expect(el().querySelector('.remove-link')).not.toBeNull();
  });

  it('shows the short row in the drawer', () => {
    render(line, true);

    expect(el().querySelector('.cart-item.compact')).not.toBeNull();
    expect(el().querySelector('.badge-accent')).toBeNull();
    expect(el().querySelector('.line-total')).toBeNull();
    expect(el().querySelector('.remove-icon')?.getAttribute('aria-label')).toBe(
      'Remove Mug from cart',
    );
  });

  it('shows the quantity as read-only text', () => {
    render(line);

    expect(el().querySelector('.qty-display')?.textContent).toContain('2');
    expect(el().querySelector('.qty-btn')).toBeNull();
  });

  it('removes the line with the Remove link', () => {
    render(line);

    (el().querySelector('.remove-link') as HTMLButtonElement).click();

    expect(cartService.removeItem).toHaveBeenCalledWith('a');
    expect(cartItems()).toEqual([]);
  });

  it('removes the line with the drawer icon', () => {
    render(line, true);

    (el().querySelector('.remove-icon') as HTMLButtonElement).click();

    expect(cartService.removeItem).toHaveBeenCalledWith('a');
    expect(cartItems()).toEqual([]);
  });

  it('keeps the line and shows an error when removing fails', () => {
    cartService.removeItem.and.returnValue(throwError(() => new Error('failed')));
    render(line);

    (el().querySelector('.remove-link') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(cartItems().map((item) => item.id)).toEqual(['a']);
    expect(el().querySelector('.remove-error')).not.toBeNull();
  });

  it('marks an out-of-stock line', () => {
    render({ ...line, stock: 0 });

    expect(el().querySelector('.cart-item.is-out')).not.toBeNull();
    expect(el().querySelector('.badge-danger')?.textContent).toContain('Out of stock');
    expect(el().querySelector('.line-total')).toBeNull();
  });
});
