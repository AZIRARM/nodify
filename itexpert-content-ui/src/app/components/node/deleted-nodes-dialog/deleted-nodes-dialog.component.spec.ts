import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeletedNodesDialogComponent } from './deleted-nodes-dialog.component';

describe('DeletedNodesDialogComponent', () => {
  let component: DeletedNodesDialogComponent;
  let fixture: ComponentFixture<DeletedNodesDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [DeletedNodesDialogComponent]
    });
    fixture = TestBed.createComponent(DeletedNodesDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
