import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/auth/services/auth.service';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authService: jasmine.SpyObj<AuthService>;
  let router: Router;
  let navigate: jasmine.Spy;
  let snackBar: MatSnackBar;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParams: of({ email: 'new.user@example.com' }) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    snackBar = (component as any).snackBar;
    spyOn(snackBar, 'open');
    router = TestBed.inject(Router);
    navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    fixture.detectChanges();
  });

  it('prefills the email from query parameters', () => {
    expect(component.loginForm.get('email')?.value).toBe('new.user@example.com');
  });

  it('does not attempt login while the form is invalid', () => {
    component.onSubmit();

    expect(authService.login).not.toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  });

  it('logs in, shows a confirmation, and redirects home', async () => {
    authService.login.and.returnValue(
      of({ id: 'user-1', name: 'User', email: 'user@example.com', role: 'CLIENT' }),
    );
    component.loginForm.setValue({ email: 'user@example.com', password: 'secret1' });
    expect(component.loginForm.valid).toBeTrue();
    jasmine.clock().install();

    await component.onSubmit();
    jasmine.clock().tick(100);
    jasmine.clock().uninstall();

    expect(authService.login).toHaveBeenCalledWith({ email: 'user@example.com', password: 'secret1' });
    expect(snackBar.open).toHaveBeenCalledWith('Login successful!', 'Close', { duration: 3000 });
    expect(navigate).toHaveBeenCalledWith(['/']);
    expect(component.loading()).toBeFalse();
  });

  it('shows the backend error message when login fails', async () => {
    authService.login.and.returnValue(throwError(() => ({ error: { detail: 'Invalid credentials' } })));
    component.loginForm.setValue({ email: 'user@example.com', password: 'secret1' });
    expect(component.loginForm.valid).toBeTrue();

    await component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({ email: 'user@example.com', password: 'secret1' });
    expect(snackBar.open).toHaveBeenCalledWith('Invalid credentials', 'Close', { duration: 3000 });
    expect(navigate).not.toHaveBeenCalled();
  });
});
