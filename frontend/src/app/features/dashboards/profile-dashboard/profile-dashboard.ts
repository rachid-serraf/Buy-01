import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import {
  ChartNoAxesColumn,
  CircleAlert,
  LucideAngularModule,
  PackageCheck,
  RotateCw,
  ShoppingBag,
  Wallet,
} from 'lucide-angular';

import { BuyerStats } from '../../../core/models/stats.model';
import { StatsService } from '../../../core/services/stats.service';
import { formatCategory } from '../../../shared/utils/category.utils';
import {
  CHART_COLORS,
  CHART_GRID_COLOR,
  CHART_TEXT_COLOR,
} from '../../../shared/utils/chart-theme';
import { StatCard } from '../../../shared/components/stat-card/stat-card';
import { env } from '../../../../environments/environment';

@Component({
  selector: 'app-profile-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    MatSnackBarModule,
    LucideAngularModule,
    BaseChartDirective,
    StatCard,
  ],
  templateUrl: './profile-dashboard.html',
  styleUrl: './profile-dashboard.scss',
})
export class ProfileDashboard implements OnInit {
  private readonly statsService = inject(StatsService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly icons = {
    ChartNoAxesColumn,
    CircleAlert,
    PackageCheck,
    RotateCw,
    ShoppingBag,
    Wallet,
  };

  readonly stats = signal<BuyerStats | null>(null);
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);

  readonly hasData = computed(() => {
    const stats = this.stats();
    return Boolean(
      stats &&
      (stats.ordersCount > 0 ||
        stats.mostBoughtProducts.length > 0 ||
        stats.bestProducts.length > 0 ||
        stats.topCategories.length > 0),
    );
  });

  readonly totalSpent = computed(() => this.formatMoney(this.stats()?.totalSpent ?? 0));
  readonly ordersCount = computed(() => String(this.stats()?.ordersCount ?? 0));

  readonly mostBoughtData = computed<ChartData<'bar'>>(() => {
    const rows = this.stats()?.mostBoughtProducts ?? [];

    return {
      labels: rows.map((row) => row.name),
      datasets: [
        {
          label: 'Units bought',
          data: rows.map((row) => row.quantity),
          backgroundColor: CHART_COLORS[0],
          hoverBackgroundColor: '#00439c',
          borderRadius: 6,
          maxBarThickness: 30,
        },
      ],
    };
  });

  readonly topCategoriesData = computed<ChartData<'doughnut'>>(() => {
    const rows = this.stats()?.topCategories ?? [];

    return {
      labels: rows.map((row) => formatCategory(row.category)),
      datasets: [
        {
          data: rows.map((row) => row.amountSpent),
          backgroundColor: CHART_COLORS,
          borderColor: '#ffffff',
          borderWidth: 2,
          hoverOffset: 6,
        },
      ],
    };
  });

  readonly mostBoughtOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 500 },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (context) => `${context.parsed.x ?? 0} units`,
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        ticks: { precision: 0, color: CHART_TEXT_COLOR },
        grid: { color: CHART_GRID_COLOR },
      },
      y: {
        ticks: { color: CHART_TEXT_COLOR },
        grid: { display: false },
      },
    },
  };

  readonly topCategoriesOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '62%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: CHART_TEXT_COLOR,
          usePointStyle: true,
          boxWidth: 8,
          padding: 16,
        },
      },
      tooltip: {
        callbacks: {
          label: (context) => `${context.label}: ${this.formatMoney(Number(context.parsed))}`,
        },
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
      .getBuyerStats()
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

  formatCategoryName(category: string): string {
    return formatCategory(category) || category;
  }

  imageSrc(imageUrl?: string | null): string | null {
    if (!imageUrl) return null;
    if (/^(https?:|data:|blob:)/i.test(imageUrl) || imageUrl.startsWith('/')) {
      return imageUrl;
    }
    return `${env.mediaUrl}/${imageUrl}`;
  }

  private readError(error: unknown): string {
    const message = (error as { error?: { message?: unknown } } | null)?.error?.message;
    return typeof message === 'string' && message.trim()
      ? message
      : 'Could not load your analytics. Please try again.';
  }
}
