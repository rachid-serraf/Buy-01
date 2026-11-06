import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../../core/auth/services/auth.service';
import {
  LucideAngularModule,
  CircleUserRound,
  X,
  Menu,
  Plus,
  LogOut,
  LayoutDashboard,
  House,
  PackagePlus,
  ShoppingBag,
} from 'lucide-angular';
import { filter } from 'rxjs';
import { env } from '../../../../environments/environment';
import { cartBadgeBouncing, cartCount, cartDrawerOpen, openCartDrawer } from '../../../cart/cart-ui.state';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatMenuModule,
    LucideAngularModule,
  ],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
})
export class NavbarComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly CircleUserRound = CircleUserRound;
  readonly X = X;
  readonly Menu = Menu;
  readonly Plus = Plus;
  readonly LogOut = LogOut;
  readonly LayoutDashboard = LayoutDashboard;
  readonly Home = House;
  readonly PackagePlus = PackagePlus;
  readonly ShoppingBag = ShoppingBag;

  readonly cartCount = cartCount;
  readonly cartDrawerOpen = cartDrawerOpen;
  readonly cartBadgeBouncing = cartBadgeBouncing;

  env = env;

  currentUser = computed(() => this.authService.user);
  isLoggedIn = computed(() => this.authService.isAuthenticated);
  isSeller = computed(() => this.authService.isSeller);

  currentUrl: string = '';
  mobileMenuOpen = false;

  ngOnInit(): void {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.currentUrl = event.urlAfterRedirects;
      });
  }

  // Mobile menu state

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
  }

  openCart(): void {
    this.closeMobileMenu();
    openCartDrawer();
  }

  logout(): void {
    this.authService.logout();
    this.closeMobileMenu();
  }
}
