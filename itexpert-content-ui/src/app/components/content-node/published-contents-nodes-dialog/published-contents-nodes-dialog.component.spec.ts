import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PublishedContentsNodesDialogComponent } from './published-contents-nodes-dialog.component';

describe('PublishedContentsNodesDialogComponent', () => {
  let component: PublishedContentsNodesDialogComponent;
  let fixture: ComponentFixture<PublishedContentsNodesDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PublishedContentsNodesDialogComponent]
    });
    fixture = TestBed.createComponent(PublishedContentsNodesDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
