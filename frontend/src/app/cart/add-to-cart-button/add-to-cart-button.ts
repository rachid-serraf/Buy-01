import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { Check, LucideAngularModule, ShoppingCart } from 'lucide-angular';
import { addToCart, CartProduct } from '../cart-ui.state';

const LOADING_MS = 400;
const ADDED_MS = 1500;

/** Add-to-cart button with loading → "✓ Added" feedback. */
@Component({
  selector: 'app-add-to-cart-button',
  imports: [LucideAngularModule],
  templateUrl: './add-to-cart-button.html',
  styleUrl: './add-to-cart-button.scss',
})
export class AddToCartButton {
  readonly ShoppingCart = ShoppingCart;
  readonly Check = Check;

  product = input.required<CartProduct>();
  quantity = input(1);
  disabled = input(false);
  /** Existing button classes, e.g. `btn btn-secondary btn-lg`. */
  buttonClass = input('btn btn-secondary');
  iconOnly = input(false);

  status = signal<'idle' | 'loading' | 'added'>('idle');
  ariaLabel = computed(() => {
    if (!this.iconOnly()) return null;
    const name = this.product().name;
    return this.status() === 'added' ? `${name} added to cart` : `Add ${name} to cart`;
  });

  private timer?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  add(event: Event): void {
    // The shop card behind this button navigates on click.
    event.stopPropagation();
    if (this.status() !== 'idle') return;

    this.status.set('loading');
    this.timer = setTimeout(() => {
      addToCart(this.product(), this.quantity());
      this.status.set('added');
      this.timer = setTimeout(() => this.status.set('idle'), ADDED_MS);
    }, LOADING_MS);
  }
}
