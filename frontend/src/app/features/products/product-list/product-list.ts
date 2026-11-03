import { AfterViewInit, Component, ElementRef, inject, OnDestroy, OnInit, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { env } from '../../../../environments/environment';
import { Router, ActivatedRoute } from '@angular/router';
import { ProductListDto, ProductSearchParams } from '../../../core/models/product.model';
import { Category, formatCategory } from '../../../shared/utils/category.utils';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-list.html',
  styleUrls: ['./product-list.scss']
})
export class ProductList implements OnInit, OnDestroy, AfterViewInit {
  private productService = inject(ProductService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  products = signal<ProductListDto[]>([]);
  isLoading = signal(false);
  hasMore = signal(true);
  error = signal<string | null>(null);
  isMyProducts = signal(false);
  isFiltering = signal(false);
  totalResults = signal<number | null>(null);

  filters: ProductSearchParams = {
    q: '',
    category: undefined,
    minPrice: undefined,
    maxPrice: undefined,
    sort: 'newest',
  };
  readonly categories: Category[] = ['ELECTRONICS', 'CLOTHING', 'HOME', 'BOOKS', 'SPORTS', 'BEAUTY', 'TOYS', 'OTHER'];
  readonly sortOptions = [
    { value: 'newest', label: 'Newest first' },
    { value: 'oldest', label: 'Oldest first' },
    { value: 'price_asc', label: 'Price: low to high' },
    { value: 'price_desc', label: 'Price: high to low' },
    { value: 'name', label: 'Name: A to Z' },
  ] as const;

  private beforeTime = signal<string>('');
  private searchPage = signal(0);
  private observer!: IntersectionObserver;

  readonly sentinel = viewChild.required<ElementRef<HTMLDivElement>>('sentinel');

  ngAfterViewInit() {
    const el = this.sentinel();
    if (el?.nativeElement) {
      this.observer.observe(el.nativeElement);
    }
  }

  ngOnInit() {
    this.isMyProducts.set(this.route.snapshot.data['myProducts'] === true || this.router.url.startsWith('/dashboard'));

    this.loadMore();

    this.observer = new IntersectionObserver(
      entries => {
        if (entries[0]?.isIntersecting && this.hasMore() && !this.isLoading()) {
          this.loadMore();
        }
      },
      { threshold: 0.1, rootMargin: '200px' }
    );
  }

  ngOnDestroy() {
    this.observer?.disconnect();
  }

  loadMore() {
    if (this.isLoading()) return;

    this.isLoading.set(true);
    this.error.set(null);

    if (this.isFiltering()) {
      this.loadSearchPage();
      return;
    }

    const request$ = this.isMyProducts() ?
      this.productService.getMyProductsList(this.beforeTime()) :
      this.productService.getListOfProducts(this.beforeTime());

    request$.subscribe({
      next: (newProducts) => {
        if (newProducts.length === 0) {
          this.hasMore.set(false);
        } else {
          this.products.update(current => [...current, ...newProducts]);

          const lastProduct = newProducts[newProducts.length - 1];
          this.beforeTime.set(lastProduct?.createdAt ?? '');
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load products');
        this.isLoading.set(false);
        console.error(err);
      }
    });
  }

  applyFilters() {
    if (this.filters.minPrice != null && this.filters.maxPrice != null && this.filters.minPrice > this.filters.maxPrice) {
      this.error.set('Minimum price cannot be greater than maximum price.');
      return;
    }

    this.isFiltering.set(this.hasActiveFilters());
    this.products.set([]);
    this.beforeTime.set('');
    this.searchPage.set(0);
    this.totalResults.set(null);
    this.hasMore.set(true);
    this.error.set(null);
    this.loadMore();
  }

  clearFilters() {
    this.filters = { q: '', category: undefined, minPrice: undefined, maxPrice: undefined, sort: 'newest' };
    this.isFiltering.set(false);
    this.products.set([]);
    this.beforeTime.set('');
    this.searchPage.set(0);
    this.totalResults.set(null);
    this.hasMore.set(true);
    this.error.set(null);
    this.loadMore();
  }

  private loadSearchPage() {
    this.productService.searchProducts({ ...this.filters, page: this.searchPage(), size: 12 }).subscribe({
      next: result => {
        this.products.update(current => [...current, ...result.content]);
        this.searchPage.set(result.page + 1);
        this.totalResults.set(result.totalElements);
        this.hasMore.set(result.page + 1 < result.totalPages);
        this.isLoading.set(false);
      },
      error: err => {
        this.error.set('Failed to search products');
        this.isLoading.set(false);
        console.error(err);
      },
    });
  }

  private hasActiveFilters(): boolean {
    return Boolean(
      this.filters.q?.trim() ||
      this.filters.category ||
      this.filters.minPrice != null ||
      this.filters.maxPrice != null ||
      this.filters.sort !== 'newest',
    );
  }

  moreDetail(product: ProductListDto) {
    this.router.navigate(['products', product.id]);
  }

  imageUrl(id?: string) {
    if (!id) return './empty.png';
    return `${env.mediaUrl}/${id}`;
  }

  formatCategory(category?: Category | null): string {
    return formatCategory(category);
  }
}

