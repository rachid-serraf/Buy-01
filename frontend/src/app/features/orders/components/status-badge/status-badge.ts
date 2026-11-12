import { Component, computed, input } from '@angular/core';
import { OrderStatus } from '../../../../core/models/order.model';
import { statusClass, statusDescription, statusLabel } from '../../utils/order.utils';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  templateUrl: './status-badge.html',
  styleUrl: './status-badge.scss',
})
export class StatusBadge {
  readonly status = input.required<OrderStatus>();

  readonly label = computed(() => statusLabel(this.status()));
  readonly description = computed(() => statusDescription(this.status()));
  readonly badgeClass = computed(() => statusClass(this.status()));
}
