import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import {
  ArrowRight,
  CircleAlert,
  LucideAngularModule,
  PackageOpen,
  RotateCcw,
  RotateCw,
  Search,
  SearchX,
  ShoppingBag,
  Store,
  Trash2,
  X,
} from 'lucide-angular';
import { debounceTime, distinctUntilChanged, filter, finalize, firstValueFrom, take } from 'rxjs';
import { AuthService } from '../../../core/auth/services/auth.service';
import { Order, OrderStatus } from '../../../core/models/order.model';
import { Role } from '../../../core/models/user.model';
import { OrderService } from '../../../core/services/order.service';
import { StatusBadge } from '../components/status-badge/status-badge';
import {
  ConfirmOrderActionData,
  ConfirmOrderActionDialog,
} from '../dialogs/confirm-order-action/confirm-order-action';
import {
  canCancelOrder,
  canRedoOrder,
  canRemoveOrder,
  formatMoney,
  formatOrderDate,
  nextOrderStatus,
  ORDER_STATUS_OPTIONS,
  orderItemsCount,
  readApiError,
  statusLabel,
} from '../utils/order.utils';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    LucideAngularModule,
    MatTableModule,
    MatPaginatorModule,
    MatDialogModule,
    MatSnackBarModule,
    StatusBadge,
  ],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss',
})
export class OrderList implements OnInit {
  readonly icons = {
    ArrowRight,
    CircleAlert,
    PackageOpen,
    RotateCcw,
    RotateCw,
    Search,
    SearchX,
    ShoppingBag,
    Store,
    Trash2,
    X,
  };

  private readonly auth = inject(AuthService);
  private readonly orderService = inject(OrderService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly statusOptions = ORDER_STATUS_OPTIONS;
  readonly displayedColumns = ['id', 'createdAt', 'itemsCount', 'total', 'status', 'actions'];
  readonly skeletonRows = [1, 2, 3, 4, 5];

  readonly filterForm = this.fb.nonNullable.group({
    search: [''],
    status: [''],
    from: [''],
    to: [''],
  });

  readonly role = computed<Role>(() => this.auth.user?.role ?? 'CLIENT');
  readonly isSeller = computed(() => this.role() === 'SELLER');

  readonly orders = signal<Order[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly isLoading = signal(true);
  readonly hasLoaded = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly dateError = signal<string | null>(null);
  readonly filtersActive = signal(false);
  readonly busyOrderId = signal<string | null>(null);

  readonly formatDate = formatOrderDate;
  readonly formatMoney = formatMoney;
  readonly itemsCount = orderItemsCount;

  private lastQueryKey = '';

  ngOnInit(): void {
    this.filterForm.valueChanges
      .pipe(
        debounceTime(350),
        distinctUntilChanged((previous, current) => {
          return JSON.stringify(previous) === JSON.stringify(current);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.applyFilters());

    this.auth.currentUser$
      .pipe(
        filter((user) => user !== null),
        take(1),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.loadOrders(true));
  }

  applyFilters(): void {
    const { search, status, from, to } = this.filterForm.getRawValue();

    this.filtersActive.set(Boolean(search.trim() || status || from || to));

    if (from && to && new Date(from).getTime() > new Date(to).getTime()) {
      this.dateError.set('The start date must be on or before the end date.');
      return;
    }

    this.dateError.set(null);
    this.pageIndex.set(0);
    this.loadOrders();
  }

  clearFilters(): void {
    this.filterForm.reset({ search: '', status: '', from: '', to: '' }, { emitEvent: false });
    this.filtersActive.set(false);
    this.dateError.set(null);
    this.pageIndex.set(0);
    this.loadOrders(true);
  }

  retry(): void {
    this.loadOrders(true);
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadOrders(true);
  }

  loadOrders(force = false): void {
    const query = this.buildQuery();
    const key = `${this.role()}|${JSON.stringify(query)}`;

    if (!force && key === this.lastQueryKey) return;

    this.lastQueryKey = key;
    this.dateError.set(null);
    this.errorMessage.set(null);
    this.isLoading.set(true);

    const request$ = this.isSeller()
      ? this.orderService.getSellerOrders(query)
      : this.orderService.getMyOrders(query);

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (page) => {
        this.orders.set(page.content);
        this.totalElements.set(page.totalElements);
        this.pageIndex.set(page.page);
        if (page.size > 0) this.pageSize.set(page.size);
        this.isLoading.set(false);
        this.hasLoaded.set(true);
      },
      error: (error: unknown) => {
        const message = readApiError(error, 'Could not load your orders.');
        this.errorMessage.set(message);
        this.isLoading.set(false);
        this.hasLoaded.set(true);
        this.notifyError(message);
      },
    });
  }

  canCancel(order: Order): boolean {
    return canCancelOrder(order, this.role());
  }

  canRedo(order: Order): boolean {
    return canRedoOrder(order, this.role());
  }

  canRemove(order: Order): boolean {
    return canRemoveOrder(order, this.role());
  }

  forwardAction(order: Order): { status: OrderStatus; label: string } | null {
    return nextOrderStatus(order, this.role());
  }

  async cancelOrder(order: Order): Promise<void> {
    const confirmed = await this.confirm({
      title: 'Cancel this order?',
      message: `Order #${order.id} will be cancelled. This action cannot be undone.`,
      confirmLabel: 'Cancel order',
      cancelLabel: 'Keep order',
      destructive: true,
    });

    if (!confirmed) return;

    this.runAction(order, this.orderService.cancelOrder(order.id), 'Order cancelled.');
  }

  redoOrder(order: Order): void {
    this.runAction(order, this.orderService.redoOrder(order.id), 'Order placed again.');
  }

  async removeOrder(order: Order): Promise<void> {
    const confirmed = await this.confirm({
      title: 'Remove this order?',
      message: `Order #${order.id} will be permanently removed from your order history. This action cannot be undone.`,
      confirmLabel: 'Remove order',
      cancelLabel: 'Keep order',
      destructive: true,
    });

    if (!confirmed) return;

    if (this.orders().length === 1 && this.pageIndex() > 0) {
      this.pageIndex.update((page) => page - 1);
    }

    this.runAction(order, this.orderService.removeOrder(order.id), 'Order removed.');
  }

  advance(order: Order): void {
    const next = this.forwardAction(order);
    if (!next) return;

    this.runAction(
      order,
      this.orderService.updateStatus(order.id, next.status),
      `Order marked as ${statusLabel(next.status).toLowerCase()}.`,
    );
  }

  private runAction(order: Order, request$: ReturnType<OrderService['cancelOrder']>, message: string) {
    this.busyOrderId.set(order.id);

    request$
      .pipe(
        finalize(() => this.busyOrderId.set(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.snackBar.open(message, 'Dismiss', { duration: 3000 });
          this.loadOrders(true);
        },
        error: (error: unknown) => {
          this.notifyError(readApiError(error, 'Could not update this order.'));
        },
      });
  }

  private buildQuery() {
    const { search, status, from, to } = this.filterForm.getRawValue();

    return {
      search: search.trim() || undefined,
      status: (status || undefined) as OrderStatus | undefined,
      from: from || undefined,
      to: to || undefined,
      page: this.pageIndex(),
      size: this.pageSize(),
    };
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
