import { Component, computed, input, output } from '@angular/core';
import { LucideAngularModule, Minus, Plus } from 'lucide-angular';

let nextId = 0;

@Component({
  selector: 'app-quantity-stepper',
  imports: [LucideAngularModule],
  templateUrl: './quantity-stepper.html',
  styleUrl: './quantity-stepper.scss',
})
export class QuantityStepper {
  readonly Minus = Minus;
  readonly Plus = Plus;
  readonly tooltipId = `qty-tooltip-${nextId++}`;

  value = input.required<number>();
  max = input.required<number>();
  itemName = input.required<string>();
  disabled = input(false);
  compact = input(false);

  valueChange = output<number>();

  atMax = computed(() => !this.disabled() && this.value() >= this.max());

  increase(): void {
    if (!this.atMax()) this.valueChange.emit(this.value() + 1);
  }

  decrease(): void {
    this.valueChange.emit(this.value() - 1);
  }
}
