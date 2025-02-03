import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NodeAccessRolesDialogComponent } from './node-access-roles.component';

describe('NodeAccessRolesComponent', () => {
  let component: NodeAccessRolesDialogComponent;
  let fixture: ComponentFixture<NodeAccessRolesDialogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [NodeAccessRolesDialogComponent]
    });
    fixture = TestBed.createComponent(NodeAccessRolesDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
