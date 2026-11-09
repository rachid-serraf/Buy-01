import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { AuthService } from './core/auth/services/auth.service';
import { CartService } from './core/services/cart.service';
import { CartDto } from './core/models/cart.model';
import { AppComponent } from './app';
import { routes } from './app.routes';
import { cartItems } from './features/cart/cart-ui.state';

describe('AppComponent', () => {
  let currentUser$: BehaviorSubject<unknown>;
  let cartService: jasmine.SpyObj<CartService>;

  const cart: CartDto = {
    id: 'cart-1',
    userId: 'user-1',
    items: [
      {
        productId: 'product-1',
        sellerId: 'seller-1',
        name: 'Phone',
        unitPrice: 10,
        imageId: null,
        quantity: 2,
      },
    ],
  };

  beforeEach(() => {
    cartItems.set([]);
    currentUser$ = new BehaviorSubject<unknown>(null);
    cartService = jasmine.createSpyObj<CartService>('CartService', [
      'getCart',
      'addItem',
      'checkout',
      'removeItem',
    ]);
    cartService.getCart.and.returnValue(of(null));

    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: AuthService,
          useValue: {
            user: null,
            isAuthenticated: false,
            isSeller: false,
            currentUser$: currentUser$.asObservable(),
          },
        },
        { provide: CartService, useValue: cartService },
      ],
    });
  });

  it('mounts the cart drawer and toast once, next to the page', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('app-navbar')).not.toBeNull();
    expect(el.querySelector('main.content router-outlet')).not.toBeNull();
    expect(el.querySelectorAll('app-cart-drawer').length).toBe(1);
    expect(el.querySelectorAll('app-cart-toast').length).toBe(1);
  });

  it('loads the saved cart when a user session is restored', () => {
    cartService.getCart.and.returnValue(of(cart));
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    currentUser$.next({ name: 'Demo', email: 'demo@example.com', role: 'CLIENT' });
    fixture.detectChanges();

    expect(cartService.getCart).toHaveBeenCalled();
    expect(cartItems().map((item) => item.name)).toEqual(['Phone']);
  });

  it('clears the cart when the user logs out', () => {
    cartItems.set([
      {
        id: 'product-1',
        name: 'Phone',
        category: '',
        price: 10,
        quantity: 2,
        stock: 2,
        image: '',
      },
    ]);
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    currentUser$.next(null);
    fixture.detectChanges();

    expect(cartItems()).toEqual([]);
  });

  it('serves the cart page at /cart instead of the catch-all redirect', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/cart');
    fixture.detectChanges();

    expect(router.url).toBe('/cart');
    expect(fixture.nativeElement.querySelector('app-cart-page')).not.toBeNull();
  });
});
