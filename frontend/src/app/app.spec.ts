import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './core/auth/services/auth.service';
import { AppComponent } from './app';
import { routes } from './app.routes';

describe('AppComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        { provide: AuthService, useValue: { user: null, isAuthenticated: false, isSeller: false } },
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

  it('serves the cart page at /cart instead of the catch-all redirect', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const router = TestBed.inject(Router);

    await router.navigateByUrl('/cart');
    fixture.detectChanges();

    expect(router.url).toBe('/cart');
    expect(fixture.nativeElement.querySelector('app-cart-page')).not.toBeNull();
  });
});
