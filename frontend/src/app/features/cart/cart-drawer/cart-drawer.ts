import { A11yModule } from '@angular/cdk/a11y';
import { DOCUMENT } from '@angular/common';
import {
  afterNextRender,
  Component,
  effect,
  ElementRef,
  inject,
  Injector,
  viewChild,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, ShoppingBag, X } from 'lucide-angular';
import { CartItemRow } from '../cart-item/cart-item';
import {
  cartCount,
  cartDrawerOpen,
  cartItems,
  cartSubtotal,
  closeCartDrawer,
  FREE_DELIVERY_THRESHOLD,
  freeDeliveryProgress,
  freeDeliveryRemaining,
} from '../cart-ui.state';
import { PricePipe } from '../price.pipe';

@Component({
  selector: 'app-cart-drawer',
  imports: [A11yModule, CartItemRow, LucideAngularModule, PricePipe, RouterLink],
  templateUrl: './cart-drawer.html',
  styleUrl: './cart-drawer.scss',
  host: { '(document:keydown.escape)': 'close()' },
})
export class CartDrawer {
  readonly X = X;
  readonly ShoppingBag = ShoppingBag;

  readonly open = cartDrawerOpen;
  readonly items = cartItems;
  readonly count = cartCount;
  readonly subtotal = cartSubtotal;
  readonly remaining = freeDeliveryRemaining;
  readonly progress = freeDeliveryProgress;
  readonly threshold = FREE_DELIVERY_THRESHOLD;

  private readonly document = inject(DOCUMENT);
  private readonly injector = inject(Injector);
  private readonly router = inject(Router);
  private readonly closeButton = viewChild.required<ElementRef<HTMLElement>>('closeButton');
  private returnFocusTo: HTMLElement | null = null;

  constructor() {
    effect(() => {
      const root = this.document.documentElement;
      if (this.open()) {
        const scrollbarWidth = window.innerWidth - root.clientWidth;
        root.style.overflow = 'hidden';
        root.style.paddingRight = scrollbarWidth ? `${scrollbarWidth}px` : '';
        this.returnFocusTo = this.document.activeElement as HTMLElement | null;
        afterNextRender(() => this.closeButton().nativeElement.focus(), {
          injector: this.injector,
        });
      } else {
        root.style.overflow = '';
        root.style.paddingRight = '';
        this.returnFocusTo?.focus();
        this.returnFocusTo = null;
      }
    });
  }

  close(): void {
    closeCartDrawer();
  }

  checkout(): void {
    closeCartDrawer();
    this.router.navigate(['/cart'], { queryParams: { checkout: '1' } });
  }
}
