import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PasswordDialogComponent } from './password-dialog.component';

describe('UserDialogComponent', () => {
  let component: PasswordDialogComponent;
  let fixture: ComponentFixture<PasswordDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PasswordDialogComponent]
    });
    fixture = TestBed.createComponent(PasswordDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
