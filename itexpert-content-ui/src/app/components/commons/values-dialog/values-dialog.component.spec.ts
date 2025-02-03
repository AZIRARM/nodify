import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ValuesDialogComponent } from './values-dialog.component';

describe('ValuesDialogComponent', () => {
  let component: ValuesDialogComponent;
  let fixture: ComponentFixture<ValuesDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ValuesDialogComponent]
    });
    fixture = TestBed.createComponent(ValuesDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
