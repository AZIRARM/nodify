import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeletedContentsNodesDialogComponent } from './deleted-contents-nodes-dialog.component';

describe('DeletedContentsNodesDialogComponent', () => {
  let component: DeletedContentsNodesDialogComponent;
  let fixture: ComponentFixture<DeletedContentsNodesDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [DeletedContentsNodesDialogComponent]
    });
    fixture = TestBed.createComponent(DeletedContentsNodesDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
