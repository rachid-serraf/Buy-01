import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { CartDrawer } from './features/cart/cart-drawer/cart-drawer';
import { CartToast } from './features/cart/cart-toast/cart-toast';
import { AuthService } from './core/auth/services/auth.service';
import { CartService } from './core/services/cart.service';
import { cartDtoToItems, cartItems } from './features/cart/cart-ui.state';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, CartDrawer, CartToast],
  template: `
    <app-navbar></app-navbar>
    <main class="content">
      <router-outlet></router-outlet>
    </main>
    <app-cart-drawer></app-cart-drawer>
    <app-cart-toast></app-cart-toast>
  `,
  styleUrl: './app.css',
})
export class AppComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.authService.currentUser$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((user) => {
        if (user) {
          this.loadCart();
        } else {
          cartItems.set([]);
        }
      });
  }

  private loadCart(): void {
    this.cartService
      .getCart()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cart) => cartItems.set(cartDtoToItems(cart)),
        error: () => {
          // The cart page surfaces load errors; the shell stays quiet.
        },
      });
  }
}
