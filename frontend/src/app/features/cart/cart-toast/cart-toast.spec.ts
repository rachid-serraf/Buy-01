import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { cartToast, hideCartToast } from '../cart-ui.state';
import { CartToast } from './cart-toast';

describe('CartToast', () => {
  let fixture: ComponentFixture<CartToast>;

  const el = () => fixture.nativeElement as HTMLElement;

  beforeEach(() => {
    hideCartToast();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    fixture = TestBed.createComponent(CartToast);
    fixture.detectChanges();
  });

  afterEach(() => hideCartToast());

  it('keeps the live region in the page while it is empty', () => {
    expect(el().querySelector('[role="status"]')).not.toBeNull();
    expect(el().querySelector('.toast')).toBeNull();
  });

  it('shows the product that was added', () => {
    cartToast.set({
      id: 'a',
      name: 'Mug',
      category: 'Home',
      price: 18,
      stock: 5,
      image: 'data:,',
    });
    fixture.detectChanges();

    expect(el().querySelector('.toast-title')?.textContent).toContain('Added to cart');
    expect(el().querySelector('.toast-name')?.textContent).toContain('Mug');
    expect(el().querySelector('.toast-thumb')?.getAttribute('src')).toBe('data:,');
  });

  it('hides when View cart is clicked', () => {
    cartToast.set({
      id: 'a',
      name: 'Mug',
      category: 'Home',
      price: 18,
      stock: 5,
      image: 'data:,',
    });
    fixture.detectChanges();

    (el().querySelector('.toast-link') as HTMLAnchorElement).click();
    fixture.detectChanges();

    expect(el().querySelector('.toast')).toBeNull();
  });
});
