import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import {
  ChevronRight,
  CircleAlert,
  Lock,
  LucideAngularModule,
  RotateCcw,
  ShoppingBag,
  Truck,
} from 'lucide-angular';
import { CartItemIssue } from '../../../core/models/cart.model';
import { OrderAddress } from '../../../core/models/order.model';
import { CartService } from '../../../core/services/cart.service';
import { openDeliveryAddressDialog } from '../../orders/dialogs/delivery-address-form/delivery-address-form';
import { CartItemRow } from '../cart-item/cart-item';
import {
  cartCount,
  cartDelivery,
  cartDtoToItems,
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
export class CartPage implements OnInit {
  readonly icons = { ChevronRight, CircleAlert, Lock, RotateCcw, ShoppingBag, Truck };

  // TODO buy-02: remove. Design preview only: /cart?state=empty and /cart?state=loading
  state = input<'empty' | 'loading'>();

  readonly items = cartItems;
  readonly count = cartCount;
  readonly subtotal = cartSubtotal;
  readonly delivery = cartDelivery;
  readonly total = cartTotal;
  readonly skeletonRows = [1, 2, 3];

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly checkoutLoading = signal(false);
  readonly checkoutIssues = signal<CartItemIssue[]>([]);
  readonly checkoutMessage = signal<string | null>(null);

  view = computed(() => {
    if (this.state() === 'loading' || this.loading()) return 'loading';
    return this.state() === 'empty' || this.items().length === 0 ? 'empty' : 'items';
  });

  promoMessage = signal('');

  private readonly cartService = inject(CartService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private lastAddress: OrderAddress | null = null;

  ngOnInit(): void {
    if (this.state()) return;
    this.loadCart(true);
  }

  loadCart(autoCheckout = false): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.cartService
      .getCart()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cart) => {
          cartItems.set(cartDtoToItems(cart));
          this.loading.set(false);

          if (autoCheckout && this.route.snapshot.queryParamMap.get('checkout') === '1') {
            this.router.navigate([], {
              relativeTo: this.route,
              queryParams: {},
              replaceUrl: true,
            });
            this.checkout();
          }
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.readError(error, 'Could not load your cart.'));
          this.loading.set(false);
        },
      });
  }

  async checkout(): Promise<void> {
    if (this.checkoutLoading() || this.items().length === 0) return;

    const address = await openDeliveryAddressDialog(this.dialog, this.lastAddress);
    if (!address) return;
    this.lastAddress = address;

    this.checkoutLoading.set(true);
    this.errorMessage.set(null);
    this.checkoutIssues.set([]);
    this.checkoutMessage.set(null);

    this.cartService
      .checkout(address)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          cartItems.set(cartDtoToItems(response.cart));
          this.checkoutIssues.set(response.issues);
          this.checkoutMessage.set(response.valid ? 'Order placed successfully.' : null);
          this.checkoutLoading.set(false);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.readError(error, 'Could not validate your cart.'));
          this.checkoutLoading.set(false);
        },
      });
  }

  private readError(error: unknown, fallback: string): string {
    const body = (error as { error?: { detail?: string; message?: string } })?.error;
    return body?.detail || body?.message || fallback;
  }
}
