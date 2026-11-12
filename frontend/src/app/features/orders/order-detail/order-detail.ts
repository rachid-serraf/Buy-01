import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  ChevronRight,
  CircleAlert,
  CreditCard,
  Image as ImageIcon,
  LucideAngularModule,
  MapPin,
  RotateCcw,
  RotateCw,
  X,
} from 'lucide-angular';
import { finalize, firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/services/auth.service';
import { Order } from '../../../core/models/order.model';
import { Role } from '../../../core/models/user.model';
import { OrderService } from '../../../core/services/order.service';
import { env } from '../../../../environments/environment';
import { StatusBadge } from '../components/status-badge/status-badge';
import { StatusTimeline } from '../components/status-timeline/status-timeline';
import {
  ConfirmOrderActionData,
  ConfirmOrderActionDialog,
} from '../dialogs/confirm-order-action/confirm-order-action';
import {
  canCancelOrder,
  canRedoOrder,
  formatAddressLines,
  formatMoney,
  formatOrderDateTime,
  nextOrderStatus,
  orderDeliveryFee,
  orderItemsCount,
  orderSubtotal,
  readApiError,
  statusDescription,
  statusLabel,
} from '../utils/order.utils';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [
    RouterLink,
    LucideAngularModule,
    MatDialogModule,
    MatSnackBarModule,
    StatusBadge,
    StatusTimeline,
  ],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss',
})
export class OrderDetail implements OnInit {
  readonly icons = {
    ChevronRight,
    CircleAlert,
    CreditCard,
    ImageIcon,
    MapPin,
    RotateCcw,
    RotateCw,
    X,
  };

  readonly id = input.required<string>();

  private readonly auth = inject(AuthService);
  private readonly orderService = inject(OrderService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly order = signal<Order | null>(null);
  readonly isLoading = signal(true);
  readonly isBusy = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly role = computed<Role>(() => this.auth.user?.role ?? 'CLIENT');

  readonly forward = computed(() => {
    const order = this.order();
    return order ? nextOrderStatus(order, this.role()) : null;
  });

  readonly canCancel = computed(() => {
    const order = this.order();
    return Boolean(order && canCancelOrder(order, this.role()));
  });

  readonly canRedo = computed(() => {
    const order = this.order();
    return Boolean(order && canRedoOrder(order, this.role()));
  });

  readonly hasActions = computed(
    () => Boolean(this.forward() || this.canCancel() || this.canRedo()),
  );

  readonly itemsSubtotal = computed(() => {
    const order = this.order();
    return order ? orderSubtotal(order) : 0;
  });

  readonly deliveryFee = computed(() => {
    const order = this.order();
    return order ? orderDeliveryFee(order) : 0;
  });

  readonly itemsCount = computed(() => {
    const order = this.order();
    return order ? orderItemsCount(order) : 0;
  });

  readonly addressLines = computed(() => formatAddressLines(this.order()?.address));

  readonly subtitle = computed(() => {
    const order = this.order();
    return order ? statusDescription(order.status) : '';
  });

  readonly formatDate = formatOrderDateTime;
  readonly formatMoney = formatMoney;

  ngOnInit(): void {
    this.loadOrder();
  }

  loadOrder(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.orderService
      .getOrder(this.id())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.order.set(order);
          this.isLoading.set(false);
        },
        error: (error: unknown) => {
          const message = readApiError(error, 'Could not load this order.');
          this.errorMessage.set(message);
          this.isLoading.set(false);
          this.notifyError(message);
        },
      });
  }

  imageSrc(imageUrl?: string | null): string | null {
    if (!imageUrl) return null;
    if (/^(https?:|data:|blob:)/i.test(imageUrl) || imageUrl.startsWith('/')) {
      return imageUrl;
    }
    return `${env.mediaUrl}/${imageUrl}`;
  }

  advance(): void {
    const order = this.order();
    const next = this.forward();
    if (!order || !next) return;

    this.runAction(
      this.orderService.updateStatus(order.id, next.status),
      `Order marked as ${statusLabel(next.status).toLowerCase()}.`,
    );
  }

  async cancel(): Promise<void> {
    const order = this.order();
    if (!order) return;

    const confirmed = await this.confirm({
      title: 'Cancel this order?',
      message: `Order #${order.id} will be cancelled. This action cannot be undone.`,
      confirmLabel: 'Cancel order',
      cancelLabel: 'Keep order',
      destructive: true,
    });

    if (!confirmed) return;

    this.runAction(this.orderService.cancelOrder(order.id), 'Order cancelled.');
  }

  redo(): void {
    const order = this.order();
    if (!order) return;

    this.runAction(this.orderService.redoOrder(order.id), 'Order placed again.');
  }

  private runAction(request$: ReturnType<OrderService['cancelOrder']>, message: string): void {
    this.isBusy.set(true);

    request$
      .pipe(
        finalize(() => this.isBusy.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(message, 'Dismiss', { duration: 3000 });
          this.loadOrder();
        },
        error: (error: unknown) => {
          this.notifyError(readApiError(error, 'Could not update this order.'));
        },
      });
  }

  private confirm(data: ConfirmOrderActionData): Promise<boolean> {
    const dialogRef = this.dialog.open<
      ConfirmOrderActionDialog,
      ConfirmOrderActionData,
      boolean
    >(ConfirmOrderActionDialog, {
      width: '400px',
      data,
      autoFocus: 'first-tabbable',
    });

    return firstValueFrom(dialogRef.afterClosed()).then((result) => result === true);
  }

  private notifyError(message: string): void {
    this.snackBar.open(message, 'Dismiss', { duration: 5000 });
  }
}
