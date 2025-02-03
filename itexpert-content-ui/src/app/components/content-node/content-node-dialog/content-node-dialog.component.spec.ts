import { ComponentFixture, TestBed } from '@angular/core/testing';

import {ContentNodeDialogComponent} from './content-node-dialog.component';

describe('ContentNodeDialogComponent', () => {
  let component: ContentNodeDialogComponent;
  let fixture: ComponentFixture<NodeContentDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ContentNodeDialogComponent]
    });
    fixture = TestBed.createComponent(ContentNodeDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
