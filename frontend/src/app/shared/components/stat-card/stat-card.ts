import { Component, input } from '@angular/core';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './stat-card.html',
  styleUrl: './stat-card.scss',
})
export class StatCard {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly hint = input<string>('');
  readonly icon = input<LucideIconData | null>(null);
  readonly tone = input<'accent' | 'success' | 'neutral'>('accent');
  readonly loading = input(false);
}
