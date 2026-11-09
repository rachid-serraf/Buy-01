import { CommonModule } from '@angular/common';
import { Component, computed, Inject, inject, input, OnInit, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ProductService } from '../../../core/services/product.service';
import { Product, ProductListDto } from '../../../core/models/product.model';
import { ProductCard } from '../../../shared/components/product-card/product-card';
import { env } from '../../../../environments/environment';
import { Category, formatCategory } from '../../../shared/utils/category.utils';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import {
  BadgeCheck, ChevronRight, CircleAlert, CreditCard, Heart, Image as ImageIcon, LucideAngularModule, Minus,
  Pencil, Plus, RotateCcw, Share2, ShoppingBag, ShoppingCart, Store, Trash2, Truck,
} from 'lucide-angular';
import { AuthService } from '../../../core/auth/services/auth.service';
import { AddToCartButton } from '../../cart/add-to-cart-button/add-to-cart-button';
import { CartProduct } from '../../cart/cart-ui.state';

@Component({
  selector: 'app-confirm-delete-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Delete product?</h2>
    <mat-dialog-content>Do you really want to delete "<strong>{{ data.name }}</strong>"?</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-button color="warn" (click)="onConfirm()">Delete</button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDeleteDialog {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDeleteDialog>,
    @Inject(MAT_DIALOG_DATA) public data: { name: string }
  ) { }

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}

@Component({
  selector: 'app-product-detail',
  imports: [AddToCartButton, CommonModule, RouterLink, LucideAngularModule, ProductCard, MatButtonModule, MatDialogModule, MatSnackBarModule],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.scss'],
})

export class ProductDetail implements OnInit {
  readonly icons = {
    BadgeCheck, ChevronRight, CircleAlert, CreditCard, Heart, ImageIcon, Minus,
    Pencil, Plus, RotateCcw, Share2, ShoppingBag, ShoppingCart, Store, Trash2, Truck,
  };

  // ── UI-only state (no backend logic) ─────────────────────
  readonly tabs = [
    { id: 'description', label: 'Description' },
    { id: 'details', label: 'Details' },
    { id: 'shipping', label: 'Shipping & returns' },
  ] as const;
  activeTab = signal<'description' | 'details' | 'shipping'>('description');
  qty = signal(1);
  wished = signal(false);
  private recent = signal<ProductListDto[]>([]);

  /** Up to 4 other products from the same category (first page of the existing list API). */
  related = computed(() => {
    const category = this.product()?.category;
    return this.recent()
      .filter((p) => p.category === category && p.id !== this.id())
      .slice(0, 4);
  });

  /** Cart line built from the loaded product (UI only). */
  cartProduct = computed<CartProduct | null>(() => {
    const p = this.product();
    if (!p) return null;
    return {
      id: this.id(),
      name: p.name,
      category: formatCategory(p.category),
      price: p.price,
      stock: p.quantity,
      image: p.mainImage ? `${env.mediaUrl}/${p.mainImage}` : '/assets/placeholder.png',
    };
  });

  changeQty(delta: number): void {
    const max = Math.max(1, this.product()?.quantity ?? 1);
    this.qty.update((q) => Math.min(max, Math.max(1, q + delta)));
  }

  share(): void {
    navigator.clipboard?.writeText(location.href).then(
      () => this.snackBar.open('Link copied', 'Close', { duration: 2000 }),
      () => this.snackBar.open('Could not copy the link', 'Close', { duration: 2000 }),
    );
  }

  readonly productService = inject(ProductService);
  readonly router = inject(Router);
  readonly dialog = inject(MatDialog);
  readonly snackBar = inject(MatSnackBar);
  readonly authService = inject(AuthService);

  errorMessage = signal<string | null>(null)
  product = signal<Product | null>(null);
  id = input.required<string>();

  selectedImageIndex = 0;
  ngOnInit(): void {
    this.productService.getProduct(this.id()).subscribe({
      next: (p) => this.product.set(p),
      error: (e) => this.errorMessage.set(e.error.title || "unkown error")
    })
    this.productService.getListOfProducts().subscribe({
      next: (list) => this.recent.set(list),
      error: () => this.recent.set([]),
    });
  }

  get displayedMain(): string {

    const product = this.product();
    if (!product || !product.thumbnails || product.thumbnails.length === 0) {
      return "./empty.png";
    }
    const id = product.thumbnails[this.selectedImageIndex] || product.mainImage;
    return `${env.mediaUrl}/${id}`;
  }

  selectThumbnail(index: number): void {
    this.selectedImageIndex = index;
  }

  formatCategory(category?: Category | null): string {
    return formatCategory(category);
  }

  buyNow(): void {
    console.log('Buy now:', this.product.name);
    // Redirect or handle checkout
  }

  async onDeleteClick(): Promise<void> {
    const currentProduct = this.product();
    if (!currentProduct) {
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDeleteDialog, {
      width: '360px',
      data: { name: currentProduct.name },
    });

    const confirmed = await firstValueFrom(dialogRef.afterClosed());
    if (!confirmed) {
      return;
    }

    this.productService.deleteProduct(this.id()).subscribe({
      next: () => {
        this.snackBar.open('Product deleted successfully', 'Close', { duration: 3000 });
        this.router.navigate(['/products']);
      },
      error: (error: any) => {
        this.snackBar.open(error?.message || 'Failed to delete product', 'Close', { duration: 3000 });
      }
    });
  }

  imageUrl(id: string) {
    if (!id) return './empty.png';
    return `${env.mediaUrl}/${id}`;
  }
  goToEdit(): void {
    this.router.navigate([`products/${this.id()}/edit`]);
  }
}
