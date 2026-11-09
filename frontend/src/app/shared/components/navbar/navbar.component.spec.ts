import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from '../../../core/auth/services/auth.service';
import { CartItem } from '../../../features/cart/cart.mock';
import {
  cartBadgeBouncing,
  cartDrawerOpen,
  cartItems,
} from '../../../features/cart/cart-ui.state';
import { NavbarComponent } from './navbar.component';

describe('NavbarComponent', () => {
  let fixture: ComponentFixture<NavbarComponent>;
  let auth: { user: unknown; isAuthenticated: boolean; isSeller: boolean; logout: jasmine.Spy };

  const line = (id: string): CartItem => ({
    id,
    name: id,
    category: 'Home',
    price: 10,
    quantity: 1,
    stock: 5,
    image: 'data:,',
  });

  const el = () => fixture.nativeElement as HTMLElement;
  const cartButton = () => el().querySelector('.cart-btn') as HTMLButtonElement;
  const badge = () => el().querySelector('.cart-badge') as HTMLElement | null;

  function render(): void {
    fixture = TestBed.createComponent(NavbarComponent);
    fixture.componentInstance.ngOnInit();
    fixture.detectChanges();
  }

  beforeEach(() => {
    cartItems.set([line('a'), line('b'), line('c')]);
    cartDrawerOpen.set(false);
    cartBadgeBouncing.set(false);
    auth = {
      user: null,
      isAuthenticated: false,
      isSeller: false,
      logout: jasmine.createSpy('logout'),
    };

    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  });

  afterEach(() => cartDrawerOpen.set(false));

  it('shows the item count on the cart button', () => {
    render();

    expect(badge()?.textContent?.trim()).toBe('3');
    expect(cartButton().getAttribute('aria-label')).toBe('Open cart, 3 items');
  });

  it('uses the singular in the label for one item', () => {
    cartItems.set([line('a')]);
    render();

    expect(cartButton().getAttribute('aria-label')).toBe('Open cart, 1 item');
  });

  it('caps the badge at 99+', () => {
    cartItems.set(Array.from({ length: 100 }, (_, index) => line(`item-${index}`)));
    render();

    expect(badge()?.textContent?.trim()).toBe('99+');
  });

  it('hides the badge on an empty cart', () => {
    cartItems.set([]);
    render();

    expect(badge()).toBeNull();
    expect(cartButton()).not.toBeNull();
  });

  it('opens the drawer and closes the mobile menu', () => {
    render();
    fixture.componentInstance.mobileMenuOpen = true;

    cartButton().click();

    expect(cartDrawerOpen()).toBeTrue();
    expect(fixture.componentInstance.mobileMenuOpen).toBeFalse();
  });

  it('runs the bounce once and then clears it', () => {
    cartBadgeBouncing.set(true);
    render();
    expect(badge()?.classList).toContain('bounce');

    badge()?.dispatchEvent(new Event('animationend'));
    fixture.detectChanges();

    expect(cartBadgeBouncing()).toBeFalse();
    expect(badge()?.classList).not.toContain('bounce');
  });

  it('shows the cart button to guests and to signed-in users', () => {
    render();
    expect(cartButton()).not.toBeNull();
    expect(el().querySelector('.avatar-btn')).toBeNull();

    auth.isAuthenticated = true;
    auth.user = { name: 'Demo', email: 'demo@example.com', role: 'CLIENT' };
    render();

    expect(cartButton()).not.toBeNull();
    expect(el().querySelector('.avatar-btn')).not.toBeNull();
  });

  it('shows seller links to sellers only', () => {
    auth.isAuthenticated = true;
    auth.isSeller = true;
    auth.user = { name: 'Demo', email: 'demo@example.com', role: 'SELLER' };
    render();

    expect(el().querySelector('.seller-only-link')).not.toBeNull();
    expect(el().querySelector('.new-product-btn')).not.toBeNull();
  });

  it('toggles the mobile menu', () => {
    render();

    fixture.componentInstance.toggleMobileMenu();
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    expect(el().querySelector('.mobile-drawer')).not.toBeNull();

    fixture.componentInstance.closeMobileMenu();
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    expect(el().querySelector('.mobile-drawer')).toBeNull();
  });

  it('logs out and closes the mobile menu', () => {
    render();
    fixture.componentInstance.mobileMenuOpen = true;

    fixture.componentInstance.logout();

    expect(auth.logout).toHaveBeenCalled();
    expect(fixture.componentInstance.mobileMenuOpen).toBeFalse();
  });

  it('tracks the current url', async () => {
    render();

    await TestBed.inject(Router).navigateByUrl('/');

    expect(fixture.componentInstance.currentUrl).toBe('/');
  });
});
