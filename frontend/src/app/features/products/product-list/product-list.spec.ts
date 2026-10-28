import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ProductListDto } from '../../../core/models/product.model';
import { ProductService } from '../../../core/services/product.service';
import { ProductList } from './product-list';

describe('ProductList', () => {
  let fixture: ComponentFixture<ProductList>;
  let component: ProductList;
  let productService: jasmine.SpyObj<ProductService>;
  let router: jasmine.SpyObj<Router>;
  let observer: jasmine.SpyObj<IntersectionObserver>;

  const products: ProductListDto[] = [
    {
      id: 'product-1',
      name: 'Wireless Headphones',
      price: 99.99,
      image: 'headphones-image',
      category: 'ELECTRONICS',
      createdAt: '2026-10-17T10:00:00.000Z',
      badge: 'New',
    },
    {
      id: 'product-2',
      name: 'Mechanical Keyboard',
      price: 79.99,
      image: 'keyboard-image',
      category: 'ELECTRONICS',
      createdAt: '2026-10-17T09:00:00.000Z',
    },
  ];

  beforeEach(async () => {
    observer = jasmine.createSpyObj<IntersectionObserver>('IntersectionObserver', ['observe', 'disconnect']);
    (window as any).IntersectionObserver = jasmine
      .createSpy('IntersectionObserver')
      .and.returnValue(observer);

    productService = jasmine.createSpyObj<ProductService>('ProductService', [
      'getListOfProducts',
      'getMyProductsList',
    ]);
    productService.getListOfProducts.and.returnValue(of(products));
    router = jasmine.createSpyObj<Router>('Router', ['navigate'], { url: '/products' });

    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { snapshot: { data: {} } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductList);
    component = fixture.componentInstance;
  });

  it('loads and renders products on initialization', () => {
    fixture.detectChanges();

    expect(productService.getListOfProducts).toHaveBeenCalledWith('');
    expect(component.products()).toEqual(products);
    expect(observer.observe).toHaveBeenCalledWith(component.sentinel().nativeElement);
    expect(fixture.nativeElement.querySelectorAll('.product-card').length).toBe(2);
    expect(fixture.nativeElement.querySelector('.product-name').textContent.trim()).toBe(products[0].name);
  });

  it('requests the next page using the last product creation time', () => {
    const nextPage: ProductListDto[] = [
      {
        id: 'product-3',
        name: 'Ergonomic Mouse',
        price: 49.99,
        image: 'mouse-image',
        category: 'ELECTRONICS',
        createdAt: '2026-10-17T08:00:00.000Z',
      },
    ];
    productService.getListOfProducts.and.returnValues(of(products), of(nextPage));

    fixture.detectChanges();
    component.loadMore();

    expect(productService.getListOfProducts).toHaveBeenCalledWith(products[1].createdAt);
    expect(component.products()).toEqual([...products, ...nextPage]);
  });

  it('loads the current user products for dashboard routes', async () => {
    TestBed.resetTestingModule();
    productService.getMyProductsList.and.returnValue(of(products));

    await TestBed.configureTestingModule({
      imports: [ProductList],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate'], { url: '/dashboard' }) },
        { provide: ActivatedRoute, useValue: { snapshot: { data: {} } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductList);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(productService.getMyProductsList).toHaveBeenCalledWith('');
    expect(productService.getListOfProducts).not.toHaveBeenCalled();
  });

  it('shows an error when loading products fails', () => {
    productService.getListOfProducts.and.returnValue(throwError(() => new Error('Network error')));

    fixture.detectChanges();
    fixture.detectChanges();

    expect(component.error()).toBe('Failed to load products');
    expect(component.isLoading()).toBeFalse();
    expect(fixture.nativeElement.querySelector('.error-message').textContent).toContain('Failed to load products');
  });

  it('navigates to a product when its details are requested', () => {
    component.moreDetail(products[0]);

    expect(router.navigate).toHaveBeenCalledWith(['products', products[0].id]);
  });
});
