import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import {
  BadgeDollarSign,
  ChartNoAxesColumn,
  CircleAlert,
  LucideAngularModule,
  PackageCheck,
  ReceiptText,
  RotateCw,
  TrendingUp,
} from 'lucide-angular';

import { SellerStats } from '../../../core/models/stats.model';
import { StatsService } from '../../../core/services/stats.service';
import {
  CHART_COLORS,
  CHART_GRID_COLOR,
  CHART_TEXT_COLOR,
  formatMonthLabel,
} from '../../../shared/utils/chart-theme';
import { StatCard } from '../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    MatSnackBarModule,
    LucideAngularModule,
    BaseChartDirective,
    StatCard,
  ],
  templateUrl: './seller-dashboard.html',
  styleUrl: './seller-dashboard.scss',
})
export class SellerDashboard implements OnInit {
  private readonly statsService = inject(StatsService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly icons = {
    BadgeDollarSign,
    ChartNoAxesColumn,
    CircleAlert,
    PackageCheck,
    ReceiptText,
    RotateCw,
    TrendingUp,
  };

  readonly stats = signal<SellerStats | null>(null);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly hasData = computed(() => {
    const stats = this.stats();
    return Boolean(
      stats &&
      (stats.ordersCount > 0 ||
        stats.unitsSold > 0 ||
        stats.totalRevenue > 0 ||
        stats.bestSellingProducts.length > 0),
    );
  });

  readonly totalRevenue = computed(() => this.formatMoney(this.stats()?.totalRevenue ?? 0));
  readonly unitsSold = computed(() => String(this.stats()?.unitsSold ?? 0));
  readonly ordersCount = computed(() => String(this.stats()?.ordersCount ?? 0));

  readonly bestSellingData = computed<ChartData<'bar'>>(() => {
    const rows = this.stats()?.bestSellingProducts ?? [];

    return {
      labels: rows.map((row) => row.name),
      datasets: [
        {
          label: 'Units sold',
          data: rows.map((row) => row.unitsSold),
          backgroundColor: CHART_COLORS[0],
          hoverBackgroundColor: '#00439c',
          borderRadius: 6,
          maxBarThickness: 46,
        },
      ],
    };
  });

  readonly revenueByMonthData = computed<ChartData<'line'>>(() => {
    const rows = this.stats()?.revenueByMonth ?? [];

    return {
      labels: rows.map((row) => formatMonthLabel(row.month)),
      datasets: [
        {
          label: 'Revenue',
          data: rows.map((row) => row.revenue),
          borderColor: CHART_COLORS[0],
          backgroundColor: 'rgba(0, 82, 191, 0.12)',
          pointBackgroundColor: CHART_COLORS[0],
          pointBorderColor: '#ffffff',
          pointRadius: 4,
          pointHoverRadius: 6,
          borderWidth: 2,
          tension: 0.35,
          fill: true,
        },
      ],
    };
  });

  readonly bestSellingOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 500 },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (context) => `${context.parsed.y ?? 0} units sold`,
        },
      },
    },
    scales: {
      x: {
        ticks: { color: CHART_TEXT_COLOR },
        grid: { display: false },
      },
      y: {
        beginAtZero: true,
        ticks: { precision: 0, color: CHART_TEXT_COLOR },
        grid: { color: CHART_GRID_COLOR },
      },
    },
  };

  readonly revenueByMonthOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 500 },
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (context) => `Revenue: ${this.formatMoney(Number(context.parsed.y ?? 0))}`,
        },
      },
    },
    scales: {
      x: {
        ticks: { color: CHART_TEXT_COLOR },
        grid: { display: false },
      },
      y: {
        beginAtZero: true,
        ticks: {
          color: CHART_TEXT_COLOR,
          callback: (value) => this.formatCompactMoney(Number(value)),
        },
        grid: { color: CHART_GRID_COLOR },
      },
    },
  };

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.statsService
      .getSellerStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (stats) => {
          this.stats.set(stats);
          this.isLoading.set(false);
        },
        error: (error: unknown) => {
          const message = this.readError(error);
          this.errorMessage.set(message);
          this.isLoading.set(false);
          this.snackBar.open(message, 'Dismiss', { duration: 5000 });
        },
      });
  }

  formatMoney(value: number | null | undefined): string {
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(Number(value ?? 0));
  }

  formatCompactMoney(value: number): string {
    if (!Number.isFinite(value)) return '$0';

    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency: 'USD',
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(value);
  }

  private readError(error: unknown): string {
    const message = (error as { error?: { message?: unknown } } | null)?.error?.message;
    return typeof message === 'string' && message.trim()
      ? message
      : 'Could not load your analytics. Please try again.';
  }
}
