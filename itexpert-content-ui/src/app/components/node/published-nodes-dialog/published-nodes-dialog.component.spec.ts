import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PublishedNodesDialogComponent } from './published-nodes-dialog.component';

describe('PublishedNodesDialogComponent', () => {
  let component: PublishedNodesDialogComponent;
  let fixture: ComponentFixture<PublishedNodesDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PublishedNodesDialogComponent]
    });
    fixture = TestBed.createComponent(PublishedNodesDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
