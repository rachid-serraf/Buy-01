import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { Product, ProductListDto } from '../../../core/models/product.model';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/auth/services/auth.service';
import { ProductDetail } from './product-detail';
import { env } from '../../../../environments/environment';

describe('ProductDetail', () => {
  let fixture: ComponentFixture<ProductDetail>;
  let component: ProductDetail;
  let productService: jasmine.SpyObj<ProductService>;
  let router: Router;

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
    category: 'ELECTRONICS',
  };

  beforeEach(async () => {
    productService = jasmine.createSpyObj<ProductService>('ProductService', ['getProduct', 'deleteProduct', 'getListOfProducts']);
    productService.getProduct.and.returnValue(of(product));
    productService.getListOfProducts.and.returnValue(of([])); // "More from …" section

    await TestBed.configureTestingModule({
      imports: [ProductDetail],
      providers: [
        { provide: ProductService, useValue: productService },
        {
          provide: CartService,
          useValue: jasmine.createSpyObj<CartService>('CartService', ['addItem']),
        },
        provideRouter([]), // real test router: needed by routerLink
        { provide: MatDialog, useValue: jasmine.createSpyObj<MatDialog>('MatDialog', ['open']) },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']) },
        { provide: AuthService, useValue: { user: null } },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

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

  it('turns the loaded product into a cart line', () => {
    fixture.detectChanges();

    expect(component.cartProduct()).toEqual({
      id: 'product-123',
      name: 'Wireless Headphones',
      category: 'Electronics',
      price: 99.99,
      stock: 8,
      image: `${env.mediaUrl}/main-image`,
    });
  });

  it('has no cart line before the product loads', () => {
    component.product.set(null);
    expect(component.cartProduct()).toBeNull();
  });

  it('uses the placeholder for a cart line without an image', () => {
    fixture.detectChanges();
    component.product.set({ ...product, mainImage: '' });
    expect(component.cartProduct()?.image).toBe('/assets/placeholder.png');
  });

  it('keeps the chosen quantity between 1 and the stock', () => {
    fixture.detectChanges();

    component.changeQty(-1);
    expect(component.qty()).toBe(1);

    component.changeQty(100);
    expect(component.qty()).toBe(8);
  });

  it('disables add to cart when the product is out of stock', () => {
    fixture.detectChanges();
    component.product.set({ ...product, quantity: 0 });
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('app-add-to-cart-button button');
    expect(buttons.length).toBe(2);
    buttons.forEach((button: HTMLButtonElement) => expect(button.disabled).toBeTrue());
  });

  it('describes the details table for screen readers', () => {
    fixture.detectChanges();
    component.activeTab.set('details');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.details-table caption').textContent).toContain(
      'Product details',
    );
  });

  it('shows up to four other products from the same category', () => {
    const listed = (id: string, category: string): ProductListDto => ({
      id,
      name: id,
      price: 10,
      category,
      createdAt: '2026-10-01T10:00:00Z',
    });
    productService.getListOfProducts.and.returnValue(
      of([
        listed('product-123', 'ELECTRONICS'),
        listed('other-category', 'HOME'),
        ...['a', 'b', 'c', 'd', 'e'].map((id) => listed(id, 'ELECTRONICS')),
      ]),
    );

    fixture.detectChanges();

    expect(component.related().map((item) => item.id)).toEqual(['a', 'b', 'c', 'd']);
  });

  it('confirms when the product link is copied', async () => {
    spyOn(navigator.clipboard, 'writeText').and.resolveTo();
    spyOn(component.snackBar, 'open');

    component.share();
    await fixture.whenStable();

    expect(component.snackBar.open).toHaveBeenCalledWith('Link copied', 'Close', { duration: 2000 });
  });

  it('says so when the product link cannot be copied', async () => {
    spyOn(navigator.clipboard, 'writeText').and.rejectWith(new Error('denied'));
    spyOn(component.snackBar, 'open');

    component.share();
    await fixture.whenStable();

    expect(component.snackBar.open).toHaveBeenCalledWith('Could not copy the link', 'Close', {
      duration: 2000,
    });
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
