import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { Subject, throwError } from 'rxjs';
import { CartService } from '../../../core/services/cart.service';
import { CartDto } from '../../../core/models/cart.model';
import { cartItems, CartProduct, cartToast, hideCartToast } from '../cart-ui.state';
import { AddToCartButton } from './add-to-cart-button';

describe('AddToCartButton', () => {
  let fixture: ComponentFixture<AddToCartButton>;
  let cartService: jasmine.SpyObj<CartService>;
  let response$: Subject<CartDto>;

  const product: CartProduct = {
    id: 'p1',
    name: 'Desk lamp',
    category: 'Home',
    price: 45,
    stock: 3,
    image: 'data:,',
  };

  const cart: CartDto = {
    id: 'cart-1',
    userId: 'user-1',
    items: [
      {
        productId: 'p1',
        sellerId: 'seller-1',
        name: 'Desk lamp',
        unitPrice: 45,
        imageId: null,
        quantity: 1,
      },
    ],
  };

  const cartWithQuantity = (quantity: number): CartDto => ({
    ...cart,
    items: [{ ...cart.items[0], quantity }],
  });

  const line = (quantity: number) => ({
    id: 'p1',
    name: 'Desk lamp',
    category: '',
    price: 45,
    quantity,
    stock: quantity,
    image: './empty.png',
  });

  const button = () => fixture.nativeElement.querySelector('button') as HTMLButtonElement;
  const wait = (ms: number) => {
    tick(ms);
    fixture.detectChanges();
  };

  beforeEach(() => {
    cartItems.set([]);
    hideCartToast();

    response$ = new Subject<CartDto>();
    cartService = jasmine.createSpyObj<CartService>('CartService', ['addItem']);
    cartService.addItem.and.returnValue(response$.asObservable());

    TestBed.configureTestingModule({
      providers: [{ provide: CartService, useValue: cartService }],
    });

    fixture = TestBed.createComponent(AddToCartButton);
    fixture.componentRef.setInput('product', product);
    fixture.detectChanges();
  });

  afterEach(() => hideCartToast());

  it('starts idle with its label', () => {
    expect(button().textContent).toContain('Add to cart');
    expect(button().getAttribute('aria-busy')).toBe('false');
  });

  it('calls the API and goes from loading to added and back to idle', fakeAsync(() => {
    button().click();
    fixture.detectChanges();
    expect(cartService.addItem).toHaveBeenCalledWith('p1', 1);
    expect(button().textContent).toContain('Adding');
    expect(button().getAttribute('aria-busy')).toBe('true');
    expect(cartItems()).toEqual([]);

    response$.next(cart);
    fixture.detectChanges();
    expect(button().textContent).toContain('Added');
    expect(button().classList).toContain('is-added');
    expect(cartItems()).toEqual([line(1)]);
    expect(cartToast()).toEqual(product);

    wait(1499);
    expect(button().classList).toContain('is-added');

    wait(1);
    expect(button().textContent).toContain('Add to cart');
    expect(button().classList).not.toContain('is-added');
  }));

  it('shows the quantity returned by the API', fakeAsync(() => {
    fixture.componentRef.setInput('quantity', 2);
    fixture.detectChanges();

    button().click();
    response$.next(cartWithQuantity(2));
    fixture.detectChanges();

    expect(cartService.addItem).toHaveBeenCalledWith('p1', 2);
    expect(cartItems()[0].quantity).toBe(2);
  }));

  it('updates an existing line with the API quantity instead of incrementing it', fakeAsync(() => {
    cartItems.set([line(5)]);

    button().click();
    response$.next(cartWithQuantity(2));
    fixture.detectChanges();

    expect(cartItems()).toEqual([line(2)]);
  }));

  it('ignores clicks while it is busy', fakeAsync(() => {
    button().click();
    button().click();
    response$.next(cart);
    fixture.detectChanges();
    button().click();
    wait(1500);

    expect(cartService.addItem).toHaveBeenCalledTimes(1);
    expect(cartItems()[0].quantity).toBe(1);
  }));

  it('does not change the cart and shows an error when the API rejects', fakeAsync(() => {
    cartService.addItem.and.returnValue(
      throwError(() => ({ error: { detail: 'Insufficient stock' } })),
    );

    button().click();
    fixture.detectChanges();

    expect(cartItems()).toEqual([]);
    expect(button().textContent).toContain('Not added');
    expect(button().classList).toContain('is-error');
    expect(button().getAttribute('title')).toBe('Insufficient stock');

    wait(2500);
    expect(button().textContent).toContain('Add to cart');
    expect(button().classList).not.toContain('is-error');
  }));

  it('does not let the click reach the card behind it', fakeAsync(() => {
    const parentClick = jasmine.createSpy('parentClick');
    fixture.nativeElement.addEventListener('click', parentClick);

    button().click();
    response$.next(cart);
    fixture.detectChanges();

    expect(parentClick).not.toHaveBeenCalled();
  }));

  it('does not let Enter reach the card behind it', () => {
    const parentKeydown = jasmine.createSpy('parentKeydown');
    fixture.nativeElement.addEventListener('keydown', parentKeydown);

    button().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

    expect(parentKeydown).not.toHaveBeenCalled();
  });

  it('can be disabled', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();
    expect(button().disabled).toBeTrue();
  });

  it('uses the given button classes', () => {
    fixture.componentRef.setInput('buttonClass', 'btn btn-primary');
    fixture.detectChanges();
    expect(button().classList).toContain('btn-primary');
  });

  it('has no aria-label when the text is visible', () => {
    expect(button().hasAttribute('aria-label')).toBeFalse();
  });

  it('describes each state in its aria-label when it only shows an icon', fakeAsync(() => {
    fixture.componentRef.setInput('iconOnly', true);
    fixture.detectChanges();
    expect(button().getAttribute('aria-label')).toBe('Add Desk lamp to cart');
    expect(button().textContent?.trim()).toBe('');

    button().click();
    fixture.detectChanges();
    expect(button().getAttribute('aria-label')).toBe('Adding Desk lamp to cart');
    expect(button().textContent?.trim()).toBe('');

    response$.next(cart);
    fixture.detectChanges();
    expect(button().getAttribute('aria-label')).toBe('Desk lamp added to cart');
    expect(button().textContent?.trim()).toBe('');

    wait(1500);
    expect(button().getAttribute('aria-label')).toBe('Add Desk lamp to cart');
  }));

  it('cancels the pending add when it is destroyed', fakeAsync(() => {
    button().click();
    fixture.destroy();

    response$.next(cart);
    tick(2000);

    expect(cartItems()).toEqual([]);
  }));
});
