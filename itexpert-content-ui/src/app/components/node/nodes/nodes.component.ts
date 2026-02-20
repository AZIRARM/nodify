import {Component, OnDestroy, OnInit, ViewChild, AfterViewInit} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {Router} from '@angular/router';

import {Node} from "../../../modeles/Node";
import {NodeService} from "../../../services/NodeService";
import {MatTableDataSource} from "@angular/material/table";
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
import {PublishedNodesDialogComponent} from "../published-nodes-dialog/published-nodes-dialog.component";
import {TranslationsDialogComponent} from "../../commons/translations-dialog/translations-dialog.component";
import {UserService} from "../../../services/UserService";
import {UserAccessService} from "../../../services/UserAccessService";
import {AuthenticationService} from "../../../services/AuthenticationService";
import {ToastrService} from "ngx-toastr";
import {Env} from "../../../../assets/configurations/environment";
import {DeletedNodesDialogComponent} from "../deleted-nodes-dialog/deleted-nodes-dialog.component";
import {NodesViewDialogComponent} from "../nodes-view-dialog/nodes-view-dialog.component";
import { interval, Subscription } from 'rxjs';
import { MatPaginator } from '@angular/material/paginator';

@Component({
  selector: 'app-nodes',
  templateUrl: './nodes.component.html',
  styleUrls: ['./nodes.component.css']
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

  parentNode: Node | null = null;

  user: any;

  environments: Node[] = [];

  dialogRef: MatDialogRef<NodeDialogComponent>;
  validationModal: MatDialogRef<ValidationDialogComponent>;
  dialogRefValues: MatDialogRef<ValuesDialogComponent>;
  dialogRefRules: MatDialogRef<NodeRulesConditionsDialogComponent>;
  dialogRefContents: MatDialogRef<ContentNodeDialogComponent>;
  dialogRefDeleteds: MatDialogRef<DeletedNodesDialogComponent>;
  dialogRefPublished: MatDialogRef<PublishedNodesDialogComponent>;
  dialogRefTranslations: MatDialogRef<TranslationsDialogComponent>;
  dialogRefTreeNode: MatDialogRef<NodesViewDialogComponent>;

  private lockRefreshSub?: Subscription;
  private allNodes: Node[] = []; // Pour stocker tous les nodes avant filtrage

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
              private dialog: MatDialog) {
  }

  ngOnInit() {
    this.user = this.userAccessService.getCurrentUser();
    this.init();
  }

  ngAfterViewInit() {
    // Connecter la pagination après l'initialisation de la vue
    setTimeout(() => {
      if (this.paginator) {
        this.dataSource.paginator = this.paginator;

        // S'abonner aux changements de page
        this.paginator.page.subscribe(() => {
          this.pageIndex = this.paginator.pageIndex;
          this.pageSize = this.paginator.pageSize;
          this.updateDataSource();
        });
      }
    });
  }

  ngOnDestroy(): void {
    if (this.lockRefreshSub) {
      this.lockRefreshSub.unsubscribe();
    }
  }

  private initLocks(nodes: Node[]) {
    nodes.forEach((node: Node) => {
      this.lockService.getLockInfoSocket(node.code, this.authService.getAccessToken()).subscribe((lockInfo: any) => {
        node.lockInfo = lockInfo;
      });
    });
  }

  init() {
    if (this.parentNode) {
      this.nodeService.getAllByParentCodeAndStatus(this.parentNode.code, StatusEnum.SNAPSHOT).subscribe(
        (response: any) => {
          this.initLocks(response);
          this.processNodes(response);
        },
        (error) => {
          console.error('Request failed with error');
        });
    } else {
      this.nodeService.getParentsNodes(StatusEnum.SNAPSHOT).subscribe(
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
  }

  private processNodes(nodes: Node[]) {
    // Ajouter les propriétés haveContents et haveChilds
    nodes.forEach((node: any) => {
      this.haveContents(node);
      this.haveChilds(node);
    });

    // Trier les nodes
    nodes = nodes.sort((a: any, b: any) => {
      if (a.favorite && !b.favorite) return -1;
      if (!a.favorite && b.favorite) return 1;
      return a.code.localeCompare(b.code);
    });

    // Stocker tous les nodes
    this.allNodes = nodes;
    this.totalElements = nodes.length;

    // Mettre à jour la source de données
    this.updateDataSource();
    this.initEnvironments();
  }

  private updateDataSource() {
    if (this.paginator) {
      // Calculer l'index de début et de fin pour la pagination
      const startIndex = this.pageIndex * this.pageSize;
      const endIndex = startIndex + this.pageSize;

      // Extraire les nodes pour la page courante
      const pagedNodes = this.allNodes.slice(startIndex, endIndex);
      this.dataSource.data = pagedNodes;
    } else {
      this.dataSource.data = this.allNodes;
    }
  }

  // Méthode pour gérer le changement de page
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
    this.dialogRef.afterClosed()
      .subscribe(result => {
        this.save(result.data);
      });
  }

  save(node: Node) {
    node.modifiedBy = this.user.id;
    this.nodeService.save(node).subscribe(
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
    this.dialogRef.afterClosed()
      .subscribe(result => {
        if (result) {
          node = result.data;
          this.save(node);
        }
      });
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
    this.validationModal.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          this.nodeService.publish(node.code).subscribe(
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
        }
      });
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
    this.validationModal.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          this.nodeService.delete(node.code).subscribe(
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
        }
      });
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
    this.dialogRefContents.afterClosed()
      .subscribe(result => {
        if (result) {
          let contentNode: ContentNode = result.data;
          if (contentNode) {
            this.contentNodeService.save(contentNode).subscribe(
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
          }
        } else {
          this.init();
        }
      });
  }

  deleteds() {
    this.dialogRefDeleteds = this.dialog.open(DeletedNodesDialogComponent, {
        height: '80vh',
        width: '80vw',
        disableClose: true,
        data: this.parentNode
      }
    );
    this.dialogRefDeleteds.afterClosed()
      .subscribe(result => {
        this.init();
      });
  }

  subnodes(node: Node) {
    this.parentNode = node;
    this.init();
  }

  back(): void {
    if (this.parentNode && this.parentNode.parentCode) {
      this.nodeService.getNodeByCodeAndStatus(this.parentNode.code, StatusEnum.SNAPSHOT).subscribe(
        (response: any) => {
          console.log('response received : ' + response);
          if (response) {
            this.nodeService.getNodeByCodeAndStatus(response.parentCode, StatusEnum.SNAPSHOT).subscribe(
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
          } else {
            this.parentNode = null;
            this.init();
          }
        },
        (error) => {
          console.error('Request failed with error');
        });
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
    this.dialogRefRules.afterClosed()
      .subscribe(result => {
        if (result) {
          this.save(node);
        }
      });
  }

  values(node: Node) {
    this.dialogRefValues = this.dialog.open(ValuesDialogComponent, {
      data: node,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefValues.afterClosed()
      .subscribe(result => {
        if (result) {
          this.save(node);
        }
      });
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
    this.nodeService.getAllParentOrigin().subscribe(
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
  }

  gotoPublished(node: Node) {
    this.dialogRefPublished = this.dialog.open(PublishedNodesDialogComponent, {
      data: node,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefPublished.afterClosed()
      .subscribe(result => {
        if (result) {
          this.init();
        }
      });
  }

  translations(node: Node) {
    this.dialogRefTranslations = this.dialog.open(TranslationsDialogComponent, {
      data: node,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefTranslations.afterClosed()
      .subscribe(result => {
        if (result) {
          this.save(node);
        }
      });
  }

  export(element: Node, environmentCode: string) {
    this.nodeService.export(element.code, environmentCode).subscribe((data: any) => {
      const jsonString = JSON.stringify(data, null, 2);
      const blob: Blob = new Blob([jsonString], {type: 'application/json'});
      const a = document.createElement('a');
      const objectUrl = URL.createObjectURL(blob);
      a.href = objectUrl;
      a.download = element.code + ".json";
      a.click();
      URL.revokeObjectURL(objectUrl);
    });
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
      this.nodeService.import((this.parentNode ? this.parentNode.code : null), JSON.parse(content)).subscribe((data: any) => {
        this.translate.get("IMPORT_SUCCESS").subscribe((translation: string) => {
          this.toast.success(translation);
          this.init();
        });
      })
    }).bind(this);
  }

  haveChilds(element: any) {
    if (!element.hasOwnProperty("haveContent")) {
      this.nodeService.haveChilds(element.code).subscribe((response: any) => {
        element.haveChilds = !!(response && response === true);
      });
    }
  }

  haveContents(element: any) {
    if (!element.hasOwnProperty("haveContent")) {
      this.nodeService.haveContents(element.code).subscribe((response: any) => {
        element.haveContent = !!(response && response === true);
      });
    }
  }

  view(element: Node) {
    window.open(Env.EXPERT_CONTENT_API_URL + "/nodes/code/"
      + element.code + '?status=' + StatusEnum.SNAPSHOT, element.code);
  }

  deploy(element: any, environmentCode: string) {
    this.nodeService.deploy(element.code, environmentCode).subscribe((data: any) => {
      this.translate.get("DEPLOY_SUCCESS").subscribe((translation: string) => {
        this.toast.success(translation);
      });
    });
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
    this.dialogRefTreeNode.afterClosed()
      .subscribe(result => {
        this.init();
      });
  }

  canEdit(node: Node): boolean {
    return node.lockInfo && !node.lockInfo.locked;
  }
}
