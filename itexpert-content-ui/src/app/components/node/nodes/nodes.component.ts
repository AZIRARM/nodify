import {Component, OnDestroy, OnInit, ViewChild, AfterViewInit, ChangeDetectorRef} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {Router} from '@angular/router';

import {Node} from "../../../modeles/Node";
import {NodeService} from "../../../services/NodeService";
import {MatTableDataSource, MatTable} from "@angular/material/table";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {NodeDialogComponent} from "../node-dialog/node-dialog.component";
import {LoggerService} from "../../../services/LoggerService";
import {LockService} from "../../../services/LockService";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {ValuesDialogComponent} from "../../commons/values-dialog/values-dialog.component";
import {
  NodeRulesConditionsDialogComponent
} from "../../commons/node-rules-conditions-dialog/node-rules-conditions-dialog.component";
import {ContentNodeDialogComponent} from "../../content-node/content-node-dialog/content-node-dialog.component";
import {ContentNodeService} from "../../../services/ContentNodeService";
import {ContentNode} from "../../../modeles/ContentNode";
import {StatusEnum} from "../../../modeles/StatusEnum";
import {PublishedItemsDialogComponent} from "../../commons/published-items-dialog/published-items-dialog.component";
import {TranslationsDialogComponent} from "../../commons/translations-dialog/translations-dialog.component";
import {UserService} from "../../../services/UserService";
import {UserAccessService} from "../../../services/UserAccessService";
import {AuthenticationService} from "../../../services/AuthenticationService";
import {ToastrService} from "ngx-toastr";
import {Env} from "../../../../assets/configurations/environment";
import {DeletedItemsDialogComponent} from "../../commons/deleted-items-dialog/deleted-items-dialog.component";
import {NodesViewDialogComponent} from "../nodes-view-dialog/nodes-view-dialog.component";
import { interval, Subscription } from 'rxjs';
import { MatPaginator } from '@angular/material/paginator';

@Component({
    selector: 'app-nodes',
    templateUrl: './nodes.component.html',
    styleUrls: ['./nodes.component.css'],
    standalone: false
})
export class NodesComponent implements OnInit, OnDestroy, AfterViewInit {
  displayedColumns: string[] = ['Status', 'Name', 'Version', 'Last Modification', 'Modified by', 'Translations', 'Rules', 'Values', 'Subnodes', 'Contents', 'Publication', 'Actions'];

  dataSource: MatTableDataSource<Node> = new MatTableDataSource<Node>([]);

  // Propriétés pour la pagination
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  totalElements: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;
  pageSizeOptions: number[] = [10, 25, 50, 100];

  @ViewChild(MatTable) table!: MatTable<Node>;

  parentNode: Node | null = null;

  user: any;

  environments: Node[] = [];

  dialogRef: MatDialogRef<NodeDialogComponent>;
  validationModal: MatDialogRef<ValidationDialogComponent>;
  dialogRefValues: MatDialogRef<ValuesDialogComponent>;
  dialogRefRules: MatDialogRef<NodeRulesConditionsDialogComponent>;
  dialogRefContents: MatDialogRef<ContentNodeDialogComponent>;
  dialogRefDeleteds: MatDialogRef<DeletedItemsDialogComponent>;
  dialogRefPublished: MatDialogRef<PublishedItemsDialogComponent>;
  dialogRefTranslations: MatDialogRef<TranslationsDialogComponent>;
  dialogRefTreeNode: MatDialogRef<NodesViewDialogComponent>;

  private lockRefreshSub?: Subscription;
  private allNodes: Node[] = [];
  private subscriptions: Subscription[] = [];

  totalDeleteds: number = 33;

  constructor(private translate: TranslateService,
              private loggerService: LoggerService,
              public userAccessService: UserAccessService,
              private authService: AuthenticationService,
              private toast: ToastrService,
              private router: Router,
              private nodeService: NodeService,
              private contentNodeService: ContentNodeService,
              private userService: UserService,
              private lockService: LockService,
              private dialog: MatDialog,
              private cdr: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.user = this.userAccessService.getCurrentUser();
    this.init();
  }

  ngAfterViewInit() {
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;

      this.cdr.detectChanges();

      const pageSub = this.paginator.page.subscribe(() => {
        this.pageIndex = this.paginator.pageIndex;
        this.pageSize = this.paginator.pageSize;
        this.updateDataSource();
        this.cdr.detectChanges();
      });
      this.subscriptions.push(pageSub);
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.lockRefreshSub) {
      this.lockRefreshSub.unsubscribe();
    }
  }

  private initLocks(nodes: Node[]) {
    nodes.forEach((node: Node) => {
      const lockSub = this.lockService.getLockInfoSocket(node.code, this.authService.getAccessToken()).subscribe((lockInfo: any) => {
        node.lockInfo = lockInfo;
      });
      this.subscriptions.push(lockSub);
    });
  }

  init() {
    let requestSub: Subscription;

    if (this.parentNode) {
      requestSub = this.nodeService.getAllByParentCodeAndStatus(this.parentNode.code, StatusEnum.SNAPSHOT).subscribe(
        (response: any) => {
          this.initLocks(response);
          this.processNodes(response);
        },
        (error) => {
          console.error('Request failed with error');
        });
    } else {
      requestSub = this.nodeService.getParentsNodes(StatusEnum.SNAPSHOT).subscribe(
        (response: any) => {
          const isAdmin: boolean = this.userAccessService.isAdmin();
          if (!isAdmin) {
            response = response.filter((node: Node) => {
              return this.user &&
                Array.isArray(this.user.projects) &&
                this.user.projects.includes(node.code)
            });
          }

          this.initLocks(response);
          this.processNodes(response);
        },
        (error) => {
          console.error('Request failed with error');
          this.router.navigateByUrl("login");
        });
    }
    this.subscriptions.push(requestSub);

    const deletedSub = this.nodeService.getDeleted(this.parentNode?.code ?? '').subscribe(
        (response: any) => {
          this.totalDeleteds = response.length;
        },
        (error: any) => {
          console.error(error);
        }
      );
    this.subscriptions.push(deletedSub);
  }

  private processNodes(nodes: Node[]) {
    nodes.forEach((node: any) => {
      this.haveContents(node);
      this.haveChilds(node);
    });

    nodes = nodes.sort((a: any, b: any) => {
      if (a.favorite && !b.favorite) return -1;
      if (!a.favorite && b.favorite) return 1;
      return a.code.localeCompare(b.code);
    });

    this.allNodes = nodes;
    this.totalElements = nodes.length;
    this.updateDataSource();
    this.initEnvironments();
  }

  private updateDataSource() {
    if (this.paginator) {
      const startIndex = this.pageIndex * this.pageSize;
      const endIndex = startIndex + this.pageSize;
      const pagedNodes = this.allNodes.slice(startIndex, endIndex);
      this.dataSource.data = pagedNodes;
    } else {
      this.dataSource.data = this.allNodes;
    }
  }

  onPageChange(event: any) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updateDataSource();
  }

  update(node: Node) {
    this.dialogRef = this.dialog.open(NodeDialogComponent, {
        data: node,
        height: '80vh',
        width: '80vw',
        disableClose: true
      }
    );
    const dialogSub = this.dialogRef.afterClosed()
      .subscribe(result => {
        this.save(result.data);
      });
    this.subscriptions.push(dialogSub);
  }

  save(node: Node) {
    node.modifiedBy = this.user.id;
    const saveSub = this.nodeService.save(node).subscribe(
      response => {
        this.translate.get("SAVE_SUCCESS").subscribe(trad => {
          this.loggerService.success(trad);
          this.init();
        });
      },
      error => {
        this.translate.get("SAVE_ERROR").subscribe(error => {
          this.loggerService.error(error);
        });
      });
    this.subscriptions.push(saveSub);
  }

  create() {
    let node: Node = new Node();
    if (this.parentNode) {
      node.parentCode = this.parentNode.code;
      node.parentCodeOrigin = this.parentNode.parentCodeOrigin ? this.parentNode.parentCodeOrigin : this.parentNode.code;
    }
    node.snapshot = true;
    this.dialogRef = this.dialog.open(NodeDialogComponent, {
        data: node,
        height: '80vh',
        width: '80vw',
        disableClose: true
      }
    );
    const dialogSub = this.dialogRef.afterClosed()
      .subscribe(result => {
        if (result) {
          node = result.data;
          this.save(node);
        }
      });
    this.subscriptions.push(dialogSub);
  }

  publish(node: Node) {
    this.validationModal = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "PUBLISH_PROJECT_TITLE",
        message: "PUBLISH_PROJECT_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.validationModal.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          const publishSub = this.nodeService.publish(node.code).subscribe(
            response => {
              this.translate.get("SAVE_SUCCESS").subscribe(trad => {
                this.loggerService.success(trad);
                this.init();
              })
            },
            error => {
              this.translate.get("SAVE_ERROR").subscribe(trad1 => {
                this.translate.get("CHANGE_PROJECT_CODE_MESSAGE").subscribe(trad2 => {
                  this.loggerService.error(trad1 + ",  " + trad2);
                })
              })
            });
          this.subscriptions.push(publishSub);
        }
      });
    this.subscriptions.push(dialogSub);
  }

  delete(node: Node) {
    this.validationModal = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_NODE_TITLE",
        message: "DELETE_NODE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.validationModal.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          const deleteSub = this.nodeService.delete(node.code).subscribe(
            response => {
              this.translate.get("DELETE_SUCCESS").subscribe(trad => {
                this.loggerService.success(trad);
                this.init();
              })
            },
            error => {
              this.translate.get("DELETE_ERROR").subscribe(trad => {
                this.loggerService.error(trad);
              })
            });
          this.subscriptions.push(deleteSub);
        }
      });
    this.subscriptions.push(dialogSub);
  }

  contents(node: Node) {
    this.dialogRefContents = this.dialog.open(ContentNodeDialogComponent, {
        data: node,
        height: "calc(100%)",
        width: "calc(100%)",
        maxWidth: "100%",
        maxHeight: "100%",
        disableClose: true
      }
    );
    const dialogSub = this.dialogRefContents.afterClosed()
      .subscribe(result => {
        if (result) {
          let contentNode: ContentNode = result.data;
          if (contentNode) {
            const saveSub = this.contentNodeService.save(contentNode).subscribe(
              response => {
                this.translate.get("SAVE_SUCCESS").subscribe(trad => {
                  this.loggerService.success(trad);
                  this.init();
                })
              },
              error => {
                this.translate.get("SAVE_ERROR").subscribe(trad1 => {
                  this.translate.get("CHANGE_PROJECT_CODE_MESSAGE").subscribe(trad2 => {
                    this.loggerService.error(trad1 + ",  " + trad2);
                  })
                })
              });
            this.subscriptions.push(saveSub);
          }
        } else {
          this.init();
        }
      });
    this.subscriptions.push(dialogSub);
  }

  deleteds() {
    this.dialogRefDeleteds = this.dialog.open(DeletedItemsDialogComponent, {
        height: '80vh',
        width: '80vw',
        disableClose: true,
        data: {
          parentNode: this.parentNode,
          titleKey: 'DELETED_NODES',
          icon: 'delete_sweep',
          displayTypeColumn: true,
          deleteService: this.nodeService
        }
      }
    );
    const dialogSub = this.dialogRefDeleteds.afterClosed()
      .subscribe(result => {
        this.init();
      });
    this.subscriptions.push(dialogSub);
  }

  subnodes(node: Node) {
    this.parentNode = node;
    this.init();
  }

  back(): void {
    if (this.parentNode && this.parentNode.parentCode) {
      const nodeSub = this.nodeService.getNodeByCodeAndStatus(this.parentNode.code, StatusEnum.SNAPSHOT).subscribe(
        (response: any) => {
          console.log('response received : ' + response);
          if (response) {
            const parentSub = this.nodeService.getNodeByCodeAndStatus(response.parentCode, StatusEnum.SNAPSHOT).subscribe(
              (response2: any) => {
                if (response2) {
                  this.parentNode = response2;
                  this.init();
                } else {
                  this.parentNode = null;
                  this.init();
                }
              },
              (error) => {
                console.error('Request failed with error');
              });
            this.subscriptions.push(parentSub);
          } else {
            this.parentNode = null;
            this.init();
          }
        },
        (error) => {
          console.error('Request failed with error');
        });
      this.subscriptions.push(nodeSub);
    } else if (this.parentNode) {
      this.parentNode = null;
      this.init();
    }
  }

  rules(node: Node) {
    this.dialogRefRules = this.dialog.open(NodeRulesConditionsDialogComponent, {
      data: node,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefRules.afterClosed()
      .subscribe(result => {
        if (result) {
          this.save(node);
        }
      });
    this.subscriptions.push(dialogSub);
  }

  values(node: Node) {
    this.dialogRefValues = this.dialog.open(ValuesDialogComponent, {
      data: node,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefValues.afterClosed()
      .subscribe(result => {
        if (result) {
          this.save(node);
        }
      });
    this.subscriptions.push(dialogSub);
  }


  getPublishedIcon(element: any) {
    if (element.publicationStatus === 'PUBLISHED')
      return "green";
    else if (element.publicationStatus === 'SNAPSHOT')
      return "yellow";
    else
      return "red";
  }

  private initEnvironments() {
    const envSub = this.nodeService.getAllParentOrigin().subscribe(
      (response: any) => {
        this.environments = response.filter(
          (env: Node) => this.user!.roles.includes("ADMIN")
            || this.user!.projects.includes(env.code))
          .filter((env: Node) =>
            !this.parentNode ||
            (env.code !== this.parentNode.code &&
             env.code !== this.parentNode.parentCodeOrigin)
          );
      },
      (error) => {
        console.error('Request failed with error');
      });
    this.subscriptions.push(envSub);
  }

  gotoPublished(node: Node) {
    this.dialogRefPublished = this.dialog.open(PublishedItemsDialogComponent, {
        height: '80vh',
        width: '80vw',
        disableClose: true,
        data: {
          itemName: node.name,
          itemCode: node.code,
          itemIcon: 'folder',
          titleKey: 'NODE_PUBLICATION_HISTORY',
          displayTypeColumn: true,
          publicationService: this.nodeService
        }
      });

      const dialogSub = this.dialogRefPublished.afterClosed().subscribe(() => {
        this.init();
      });
      this.subscriptions.push(dialogSub);
  }

  translations(node: Node) {
    this.dialogRefTranslations = this.dialog.open(TranslationsDialogComponent, {
      data: node,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefTranslations.afterClosed()
      .subscribe(result => {
        if (result) {
          this.save(node);
        }
      });
    this.subscriptions.push(dialogSub);
  }

  export(element: Node, environmentCode: string) {
    const exportSub = this.nodeService.export(element.code, environmentCode).subscribe((data: any) => {
      const jsonString = JSON.stringify(data, null, 2);
      const blob: Blob = new Blob([jsonString], {type: 'application/json'});
      const a = document.createElement('a');
      const objectUrl = URL.createObjectURL(blob);
      a.href = objectUrl;
      a.download = element.code + ".json";
      a.click();
      URL.revokeObjectURL(objectUrl);
    });
    this.subscriptions.push(exportSub);
  }

  import(fileList: any) {
    if (fileList.files.length < 1) {
      return;
    }

    let file: File = fileList.files[0];
    const reader: any = new FileReader()
    reader.readAsText(file, "UTF-8");
    let content: any;
    reader.onload = ((evt: any) => {
      content = evt.target.result;
      const importSub = this.nodeService.import((this.parentNode ? this.parentNode.code : null), JSON.parse(content)).subscribe((data: any) => {
        this.translate.get("IMPORT_SUCCESS").subscribe((translation: string) => {
          this.toast.success(translation);
          this.init();
        });
      });
      this.subscriptions.push(importSub);
    }).bind(this);
  }

  haveChilds(element: any) {
    if (!element.hasOwnProperty("haveContent")) {
      const childSub = this.nodeService.haveChilds(element.code).subscribe((response: any) => {
        element.haveChilds = !!(response && response === true);
      });
      this.subscriptions.push(childSub);
    }
  }

  haveContents(element: any) {
    if (!element.hasOwnProperty("haveContent")) {
      const contentSub = this.nodeService.haveContents(element.code).subscribe((response: any) => {
        element.haveContent = !!(response && response === true);
      });
      this.subscriptions.push(contentSub);
    }
  }

  view(element: Node) {
    window.open(Env.EXPERT_CONTENT_API_URL + "/nodes/code/"
      + element.code + '?status=' + StatusEnum.SNAPSHOT, element.code);
  }

  deploy(element: any, environmentCode: string) {
    const deploySub = this.nodeService.deploy(element.code, environmentCode).subscribe((data: any) => {
      this.translate.get("DEPLOY_SUCCESS").subscribe((translation: string) => {
        this.toast.success(translation);
      });
    });
    this.subscriptions.push(deploySub);
  }

  getEnvironments() {
    if (!this.parentNode) {
      return [];
    }
    return this.environments.filter((env: Node) => env.code !== this.parentNode!.code);
  }

  favorite(element: Node) {
    element.favorite = !element.favorite;
    this.save(element);
  }

  viewTreeNode(element: Node) {
    this.dialogRefTreeNode = this.dialog.open(NodesViewDialogComponent, {
        height: '80vh',
        width: '80vw',
        disableClose: true,
        data: element
      }
    );
    const dialogSub = this.dialogRefTreeNode.afterClosed()
      .subscribe(result => {
        this.init();
      });
    this.subscriptions.push(dialogSub);
  }

  canEdit(node: Node): boolean {
    return node.lockInfo && !node.lockInfo.locked;
  }
}
