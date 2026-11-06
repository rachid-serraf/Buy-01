import { Component, computed, input } from '@angular/core';
import { LucideAngularModule, Trash2 } from 'lucide-angular';
import { CartItem } from '../cart.mock';
import { isOutOfStock, removeCartItem, setCartItemQuantity } from '../cart-ui.state';
import { PricePipe } from '../price.pipe';
import { QuantityStepper } from '../quantity-stepper/quantity-stepper';

/** One cart line: `compact` in the mini-cart drawer, full card on the cart page. */
@Component({
  selector: 'app-cart-item',
  imports: [LucideAngularModule, PricePipe, QuantityStepper],
  templateUrl: './cart-item.html',
  styleUrl: './cart-item.scss',
})
export class CartItemRow {
  readonly Trash2 = Trash2;

  item = input.required<CartItem>();
  compact = input(false);

  outOfStock = computed(() => isOutOfStock(this.item()));

  setQuantity(quantity: number): void {
    setCartItemQuantity(this.item().id, quantity);
  }

  remove(): void {
    removeCartItem(this.item().id);
  }
}
