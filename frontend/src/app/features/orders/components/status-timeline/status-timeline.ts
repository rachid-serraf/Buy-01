import { Component, computed, input } from '@angular/core';
import { LucideAngularModule, Check, Circle, XCircle } from 'lucide-angular';
import { OrderStatus } from '../../../../core/models/order.model';
import { ORDER_FLOW, statusLabel } from '../../utils/order.utils';

@Component({
  selector: 'app-status-timeline',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './status-timeline.html',
  styleUrl: './status-timeline.scss',
})
export class StatusTimeline {
  readonly Check = Check;
  readonly Circle = Circle;
  readonly XCircle = XCircle;

  readonly status = input.required<OrderStatus>();

  readonly steps = ORDER_FLOW;
  readonly isCancelled = computed(() => this.status() === 'CANCELLED');
  readonly cancelledLabel = computed(() => statusLabel('CANCELLED'));

  readonly currentIndex = computed(() =>
    ORDER_FLOW.findIndex((step) => step.status === this.status()),
  );

  readonly currentDescription = computed(
    () => ORDER_FLOW[this.currentIndex()]?.description ?? '',
  );
}
