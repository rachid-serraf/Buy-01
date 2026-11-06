import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CartItem } from '../cart.mock';
import { cartItems } from '../cart-ui.state';
import { CartPage } from './cart-page';

describe('CartPage', () => {
  let fixture: ComponentFixture<CartPage>;

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

  const el = () => fixture.nativeElement as HTMLElement;
  const text = (selector: string) => el().querySelector(selector)?.textContent?.trim();

  beforeEach(() => {
    cartItems.set([line()]);
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
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

  it('answers a promo code without reloading the page', () => {
    fixture.detectChanges();
    const input = el().querySelector('.promo input') as HTMLInputElement;
    const event = new Event('submit', { cancelable: true });

    input.value = 'SAVE10';
    el().querySelector('.promo')?.dispatchEvent(event);
    fixture.detectChanges();

    expect(event.defaultPrevented).toBeTrue();
    expect(text('.promo-message')).toBe('Promo codes are coming soon.');
  });

  it('shows no promo message for a blank code', () => {
    fixture.detectChanges();
    const input = el().querySelector('.promo input') as HTMLInputElement;

    input.value = '   ';
    el().querySelector('.promo')?.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();

    expect(text('.promo-message')).toBe('');
  });

  it('keeps the checkout buttons on the page until checkout exists', () => {
    fixture.detectChanges();

    (el().querySelector('.summary-cta') as HTMLButtonElement).click();
    (el().querySelector('.buy-bar .btn') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(el().querySelector('.summary')).not.toBeNull();
  });
});
