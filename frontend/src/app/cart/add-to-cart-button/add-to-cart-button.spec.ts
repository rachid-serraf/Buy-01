import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { cartItems, CartProduct, cartToast, hideCartToast } from '../cart-ui.state';
import { AddToCartButton } from './add-to-cart-button';

describe('AddToCartButton', () => {
  let fixture: ComponentFixture<AddToCartButton>;

  const product: CartProduct = {
    id: 'p1',
    name: 'Desk lamp',
    category: 'Home',
    price: 45,
    stock: 3,
    image: 'data:,',
  };

  const button = () => fixture.nativeElement.querySelector('button') as HTMLButtonElement;
  const wait = (ms: number) => {
    tick(ms);
    fixture.detectChanges();
  };

  beforeEach(() => {
    cartItems.set([]);
    fixture = TestBed.createComponent(AddToCartButton);
    fixture.componentRef.setInput('product', product);
    fixture.detectChanges();
  });

  afterEach(() => hideCartToast());

  it('starts idle with its label', () => {
    expect(button().textContent).toContain('Add to cart');
    expect(button().getAttribute('aria-busy')).toBe('false');
  });

  it('goes from loading to added and back to idle', fakeAsync(() => {
    button().click();
    fixture.detectChanges();
    expect(button().textContent).toContain('Adding');
    expect(button().getAttribute('aria-busy')).toBe('true');
    expect(cartItems()).toEqual([]);

    wait(400);
    expect(button().textContent).toContain('Added');
    expect(button().classList).toContain('is-added');
    expect(cartItems()).toEqual([{ ...product, quantity: 1 }]);
    expect(cartToast()).toEqual(product);

    wait(1499);
    expect(button().classList).toContain('is-added');

    wait(1);
    expect(button().textContent).toContain('Add to cart');
    expect(button().classList).not.toContain('is-added');

    hideCartToast();
  }));

  it('adds the requested quantity', fakeAsync(() => {
    fixture.componentRef.setInput('quantity', 2);
    fixture.detectChanges();

    button().click();
    wait(400);
    wait(1500);

    expect(cartItems()[0].quantity).toBe(2);
    hideCartToast();
  }));

  it('ignores clicks while it is busy', fakeAsync(() => {
    button().click();
    button().click();
    wait(400);
    button().click();
    wait(1500);

    expect(cartItems()[0].quantity).toBe(1);
    hideCartToast();
  }));

  it('does not let the click reach the card behind it', fakeAsync(() => {
    const parentClick = jasmine.createSpy('parentClick');
    fixture.nativeElement.addEventListener('click', parentClick);

    button().click();
    wait(400);
    wait(1500);

    expect(parentClick).not.toHaveBeenCalled();
    hideCartToast();
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
    expect(button().textContent?.trim()).toBe('');

    wait(400);
    expect(button().getAttribute('aria-label')).toBe('Desk lamp added to cart');
    expect(button().textContent?.trim()).toBe('');

    wait(1500);
    hideCartToast();
  }));

  it('cancels the pending add when it is destroyed', fakeAsync(() => {
    button().click();
    fixture.destroy();

    tick(2000);

    expect(cartItems()).toEqual([]);
  }));
});
