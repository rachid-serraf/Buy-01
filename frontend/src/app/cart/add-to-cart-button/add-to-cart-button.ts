import { Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Check, CircleAlert, LucideAngularModule, ShoppingCart } from 'lucide-angular';
import { CartService } from '../../core/services/cart.service';
import { addToCart, CartProduct } from '../cart-ui.state';

const ADDED_MS = 1500;
const ERROR_MS = 2500;

@Component({
  selector: 'app-add-to-cart-button',
  imports: [LucideAngularModule],
  templateUrl: './add-to-cart-button.html',
  styleUrl: './add-to-cart-button.scss',
})
export class AddToCartButton {
  readonly ShoppingCart = ShoppingCart;
  readonly Check = Check;
  readonly CircleAlert = CircleAlert;

  product = input.required<CartProduct>();
  quantity = input(1);
  disabled = input(false);
  buttonClass = input('btn btn-secondary');
  iconOnly = input(false);

  status = signal<'idle' | 'loading' | 'added' | 'error'>('idle');
  errorMessage = signal<string | null>(null);

  ariaLabel = computed(() => {
    if (!this.iconOnly()) return null;
    const name = this.product().name;
    switch (this.status()) {
      case 'added':
        return `${name} added to cart`;
      case 'error':
        return `${name} could not be added to cart`;
      case 'loading':
        return `Adding ${name} to cart`;
      default:
        return `Add ${name} to cart`;
    }
  });

  private readonly cartService = inject(CartService);
  private readonly destroyRef = inject(DestroyRef);
  private timer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.destroyRef.onDestroy(() => clearTimeout(this.timer));
  }

  add(event: Event): void {
    // The shop card behind this button navigates on click.
    event.stopPropagation();
    if (this.status() !== 'idle') return;

    this.errorMessage.set(null);
    this.status.set('loading');

    this.cartService
      .addItem(this.product().id, this.quantity())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          addToCart(this.product(), this.quantity());
          this.status.set('added');
          this.resetAfter(ADDED_MS);
        },
        error: (error: unknown) => {
          this.errorMessage.set(this.readError(error));
          this.status.set('error');
          this.resetAfter(ERROR_MS);
        },
      });
  }

  private resetAfter(ms: number): void {
    clearTimeout(this.timer);
    this.timer = setTimeout(() => {
      this.status.set('idle');
      this.errorMessage.set(null);
    }, ms);
  }

  private readError(error: unknown): string {
    const detail = (error as { error?: { detail?: string } })?.error?.detail;
    return detail || 'Could not add this item to your cart.';
  }
}
