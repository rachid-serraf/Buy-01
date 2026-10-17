import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { Product } from '../../../core/models/product.model';
import { ProductService } from '../../../core/services/product.service';
import { AuthService } from '../../../core/auth/services/auth.service';
import { ProductDetail } from './product-detail';
import { env } from '../../../../environments/environment';

describe('ProductDetail', () => {
  let fixture: ComponentFixture<ProductDetail>;
  let component: ProductDetail;
  let productService: jasmine.SpyObj<ProductService>;
  let router: jasmine.SpyObj<Router>;

  const product: Product = {
    name: 'Wireless Headphones',
    description: 'Noise-cancelling headphones.',
    price: 99.99,
    oldPrice: 120.39,
    userId: 'seller-1',
    rating: 4.8,
    reviewsCount: 124,
    quantity: 8,
    mainImage: 'main-image',
    thumbnails: ['first-image', 'second-image'],
  };

  beforeEach(async () => {
    productService = jasmine.createSpyObj<ProductService>('ProductService', ['getProduct', 'deleteProduct']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    productService.getProduct.and.returnValue(of(product));

    await TestBed.configureTestingModule({
      imports: [ProductDetail],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: Router, useValue: router },
        { provide: MatDialog, useValue: jasmine.createSpyObj<MatDialog>('MatDialog', ['open']) },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']) },
        { provide: AuthService, useValue: { user: null } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductDetail);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('id', 'product-123');
  });

  it('loads and displays the requested product', () => {
    fixture.detectChanges();

    expect(productService.getProduct).toHaveBeenCalledWith('product-123');
    expect(component.product()).toEqual(product);
    expect(fixture.nativeElement.querySelector('.product-title').textContent.trim()).toBe(product.name);
    expect(component.displayedMain).toBe(`${env.mediaUrl}/first-image`);
  });

  it('updates the main image when a thumbnail is selected', () => {
    fixture.detectChanges();

    component.selectThumbnail(1);

    expect(component.selectedImageIndex).toBe(1);
    expect(component.displayedMain).toBe(`${env.mediaUrl}/second-image`);
  });

  it('uses the placeholder image when no product image is available', () => {
    component.product.set({ ...product, thumbnails: [], mainImage: '' });

    expect(component.displayedMain).toBe('./empty.png');
    expect(component.imageUrl('')).toBe('./empty.png');
  });

  it('shows the backend error when loading the product fails', () => {
    productService.getProduct.and.returnValue(throwError(() => ({ error: { title: 'Product not found' } })));

    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Product not found');
    expect(fixture.nativeElement.querySelector('.message-error-container').textContent).toContain(
      'Product not found',
    );
  });

  it('navigates to the product edit page', () => {
    component.goToEdit();

    expect(router.navigate).toHaveBeenCalledWith(['products/product-123/edit']);
  });
});
