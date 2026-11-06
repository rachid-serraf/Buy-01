import { Component, computed, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  ChevronRight,
  Lock,
  LucideAngularModule,
  RotateCcw,
  ShoppingBag,
  Truck,
} from 'lucide-angular';
import { CartItemRow } from '../cart-item/cart-item';
import {
  cartCount,
  cartDelivery,
  cartItems,
  cartSubtotal,
  cartTotal,
} from '../cart-ui.state';
import { PricePipe } from '../price.pipe';

@Component({
  selector: 'app-cart-page',
  imports: [CartItemRow, LucideAngularModule, PricePipe, RouterLink],
  templateUrl: './cart-page.html',
  styleUrl: './cart-page.scss',
})
export class CartPage {
  readonly icons = { ChevronRight, Lock, RotateCcw, ShoppingBag, Truck };

  // TODO buy-02: remove. Design preview only: /cart?state=empty and /cart?state=loading
  state = input<'empty' | 'loading'>();

  readonly items = cartItems;
  readonly count = cartCount;
  readonly subtotal = cartSubtotal;
  readonly delivery = cartDelivery;
  readonly total = cartTotal;
  readonly skeletonRows = [1, 2, 3];

  view = computed(() => {
    if (this.state() === 'loading') return 'loading';
    return this.state() === 'empty' || this.items().length === 0 ? 'empty' : 'items';
  });

  promoMessage = signal('');

  applyPromo(event: Event, code: string): void {
    event.preventDefault();
    // TODO buy-02: validate the code through the API.
    this.promoMessage.set(code.trim() ? 'Promo codes are coming soon.' : '');
  }

  checkout(): void {
    // TODO buy-02: go to the checkout page once it exists.
  }
}
