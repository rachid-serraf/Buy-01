import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { RegisterComponent } from './register.component';
import { AuthService } from '../../../core/auth/services/auth.service';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['register']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [{ provide: AuthService, useValue: authService }, provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('starts with the client role selected and an invalid form', () => {
    expect(component.registerForm.get('role')?.value).toBe('CLIENT');
    expect(component.registerForm.invalid).toBeTrue();
    expect(component.hidePassword()).toBeTrue();
  });

  it('does not register while required fields are invalid', async () => {
    await component.onSubmit();

    expect(authService.register).not.toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  });

  it('records an error and clears the selected avatar for an unsupported file', () => {
    const file = new File(['not an image'], 'resume.pdf', { type: 'application/pdf' });
    component.selectedAvatar.set(new File(['image'], 'avatar.png', { type: 'image/png' }));
    component.avatarPreview.set('data:image/png;base64,old-preview');

    component.onAvatarSelected({ target: { files: [file] } } as unknown as Event);

    expect(component.avatarError()).toBe(
      'Invalid file type. Please upload a JPEG, PNG, WebP, or GIF file.',
    );
    expect(component.selectedAvatar()).toBeNull();
    expect(component.avatarPreview()).toBeNull();
  });

  it('clears avatar-related state when the user removes an avatar', () => {
    component.selectedAvatar.set(new File(['image'], 'avatar.png', { type: 'image/png' }));
    component.avatarPreview.set('data:image/png;base64,preview');
    component.avatarError.set('Previous error');

    component.removeAvatar();

    expect(component.selectedAvatar()).toBeNull();
    expect(component.avatarPreview()).toBeNull();
    expect(component.avatarError()).toBeNull();
  });
});
