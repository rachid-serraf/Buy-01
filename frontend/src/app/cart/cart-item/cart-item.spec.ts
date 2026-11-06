import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CartItem } from '../cart.mock';
import { cartItems } from '../cart-ui.state';
import { CartItemRow } from './cart-item';

describe('CartItemRow', () => {
  let fixture: ComponentFixture<CartItemRow>;

  const line: CartItem = {
    id: 'a',
    name: 'Mug',
    category: 'Home',
    price: 18,
    quantity: 2,
    stock: 5,
    image: 'data:,',
  };

  const el = () => fixture.nativeElement as HTMLElement;

  function render(item: CartItem, compact = false): void {
    cartItems.set([item]);
    fixture.componentRef.setInput('item', item);
    fixture.componentRef.setInput('compact', compact);
    fixture.detectChanges();
  }

  beforeEach(() => {
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

  it('updates the cart from the stepper', () => {
    render(line);

    (el().querySelectorAll('.qty-btn')[1] as HTMLButtonElement).click();

    expect(cartItems()[0].quantity).toBe(3);
  });

  it('removes the line with the Remove link', () => {
    render(line);

    (el().querySelector('.remove-link') as HTMLButtonElement).click();

    expect(cartItems()).toEqual([]);
  });

  it('removes the line with the drawer icon', () => {
    render(line, true);

    (el().querySelector('.remove-icon') as HTMLButtonElement).click();

    expect(cartItems()).toEqual([]);
  });

  it('marks an out-of-stock line and blocks its stepper', () => {
    render({ ...line, stock: 0 });

    expect(el().querySelector('.cart-item.is-out')).not.toBeNull();
    expect(el().querySelector('.badge-danger')?.textContent).toContain('Out of stock');
    expect(el().querySelector('.line-total')).toBeNull();
    el()
      .querySelectorAll<HTMLButtonElement>('.qty-btn')
      .forEach((button) => expect(button.disabled).toBeTrue());
  });
});
