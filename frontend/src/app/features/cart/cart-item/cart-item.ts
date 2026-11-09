import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucideAngularModule, Trash2 } from 'lucide-angular';
import { CartService } from '../../../core/services/cart.service';
import { CartItem } from '../cart.mock';
import { cartDtoToItems, cartItems, isOutOfStock } from '../cart-ui.state';
import { PricePipe } from '../price.pipe';

@Component({
  selector: 'app-cart-item',
  imports: [LucideAngularModule, PricePipe],
  templateUrl: './cart-item.html',
  styleUrl: './cart-item.scss',
})
export class CartItemRow {
  readonly Trash2 = Trash2;

  item = input.required<CartItem>();
  compact = input(false);

  outOfStock = computed(() => isOutOfStock(this.item()));

  readonly removing = signal(false);
  readonly removeError = signal(false);

  private readonly cartService = inject(CartService);
  private readonly destroyRef = inject(DestroyRef);

  remove(): void {
    if (this.removing()) return;

    this.removing.set(true);
    this.removeError.set(false);

    this.cartService
      .removeItem(this.item().id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (cart) => {
          cartItems.set(cartDtoToItems(cart));
          this.removing.set(false);
        },
        error: () => {
          this.removing.set(false);
          this.removeError.set(true);
        },
      });
  }
}
