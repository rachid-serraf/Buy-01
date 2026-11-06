import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Check, LucideAngularModule } from 'lucide-angular';
import { cartToast, hideCartToast } from '../cart-ui.state';

@Component({
  selector: 'app-cart-toast',
  imports: [LucideAngularModule, RouterLink],
  templateUrl: './cart-toast.html',
  styleUrl: './cart-toast.scss',
})
export class CartToast {
  readonly Check = Check;
  readonly toast = cartToast;
  readonly hide = hideCartToast;
}
