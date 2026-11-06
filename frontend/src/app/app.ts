import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { CartDrawer } from './cart/cart-drawer/cart-drawer';
import { CartToast } from './cart/cart-toast/cart-toast';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, CartDrawer, CartToast],
  template: `
    <app-navbar></app-navbar>
    <main class="content">
      <router-outlet></router-outlet>
    </main>
    <app-cart-drawer></app-cart-drawer>
    <app-cart-toast></app-cart-toast>
  `,
  styleUrl: './app.css',
})
export class AppComponent {}
