import {Component, OnInit} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {Router} from '@angular/router';

import {Node} from "../../../modeles/Node";
import {NodeService} from "../../../services/NodeService";
import {MatTableDataSource} from "@angular/material/table";
import {MatDialog, MatDialogRef} from "@angular/material/dialog";
import {NodeDialogComponent} from "../node-dialog/node-dialog.component";
import {LoggerService} from "../../../services/LoggerService";
import {ValidationDialogComponent} from "../../commons/validation-dialog/validation-dialog.component";
import {User} from "../../../modeles/User";
import {ValuesDialogComponent} from "../../commons/values-dialog/values-dialog.component";
import {NodeAccessRolesDialogComponent} from "../node-access-roles-dialg/node-access-roles-dialog.component";
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
import {ToastrService} from "ngx-toastr";
import {Env} from "../../../../assets/configurations/environment";
import {DeletedNodesDialogComponent} from "../deleted-nodes-dialog/deleted-nodes-dialog.component";

@Component({
  selector: 'app-nodes',
  templateUrl: './nodes.component.html',
  styleUrls: ['./nodes.component.css']
})
export class NodesComponent implements OnInit {
  displayedColumns: string[] = ['Status', 'Name', 'Version', 'Last Modification', 'Modified by', 'Translations', 'Rules', 'Values', 'Subnodes', 'Contents', 'Publication', 'Actions'];

  dataSource: MatTableDataSource<Node>;

  parentNode: Node;
  publishedNodes: any[];

  user: User;

  environments: Node[];

  dialogRef: MatDialogRef<NodeDialogComponent>;
  validationModal: MatDialogRef<ValidationDialogComponent>;
  dialogRefValues: MatDialogRef<ValuesDialogComponent>;
  dialogRefRules: MatDialogRef<NodeRulesConditionsDialogComponent>;
  dialogRefAccessRoles: MatDialogRef<NodeAccessRolesDialogComponent>;
  dialogRefContents: MatDialogRef<ContentNodeDialogComponent>;
  dialogRefDeleteds: MatDialogRef<DeletedNodesDialogComponent>;
  dialogRefPublished: MatDialogRef<PublishedNodesDialogComponent>;
  dialogRefTranslations: MatDialogRef<TranslationsDialogComponent>;

  constructor(private translate: TranslateService,
              private loggerService: LoggerService,
              public userAccessService: UserAccessService,
              private toast: ToastrService,
              private router: Router,
              private nodeService: NodeService,
              private contentNodeService: ContentNodeService,
              private userService: UserService,
              private dialog: MatDialog) {
  }


  ngOnInit() {
    this.user = JSON.parse(
      JSON.parse(
        JSON.stringify((window.localStorage.getItem('userInfo')))
      )
    );

    this.init();
  }

  init() {
    if (this.parentNode) {
      this.nodeService.getAllByParentCodeAndStatus(this.parentNode.code, StatusEnum.SNAPSHOT).subscribe(
        (response: any) => {
          response.map((node: any) => this.setUserName(node));
          response.map((node: any) => this.haveContents(node));
          response.map((node: any) => this.haveChilds(node));
          this.dataSource = new MatTableDataSource(response);
          this.initPublishedNodes();
          this.initEnvironments();
        },
        (error) => {
          console.error('Request failed with error');
        });
    } else {
      this.nodeService.getParentsNodes(StatusEnum.SNAPSHOT).subscribe(
        (response: any) => {
          console.log('response received : ' + response);
          if (!this.user.roles.includes("ADMIN"))
            response = response.filter((node: Node) => this.user.projects.includes(node.code));
          response.map((node: any) => this.setUserName(node));
          response.map((node: any) => this.haveContents(node));
          response.map((node: any) => this.haveChilds(node));
          this.dataSource = new MatTableDataSource(response);
          this.initPublishedNodes();
          this.initEnvironments();
        },
        (error) => {
          console.error('Request failed with error');
          this.router.navigateByUrl("login");
        });
    }
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
    node.modifiedBy = this.userAccessService.getUser().id;
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
          let isSnapshot: boolean = true;

          this.nodeService.publish(node.id, this.userAccessService.getUser().id).subscribe(
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
          this.nodeService.delete(node.code, this.userAccessService.getUser().id).subscribe(
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
        }
      });
  }


  deleteds() {
    this.dialogRefDeleteds = this.dialog.open(DeletedNodesDialogComponent, {
        height: '80vh',
        width: '80vw',
        disableClose: true
      }
    );
    this.dialogRefDeleteds.afterClosed()
      .subscribe();
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

                  this.parentNode = null!;

                  this.init();
                }
              },
              (error) => {
                console.error('Request failed with error');
              });
          } else {
            this.parentNode = null!;
            this.init();
          }

        },
        (error) => {
          console.error('Request failed with error');
        });
    } else if (this.parentNode) {
      this.parentNode = null as any;
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

  roles(node: Node) {
    this.dialogRefAccessRoles = this.dialog.open(NodeAccessRolesDialogComponent, {
      data: node,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    this.dialogRefAccessRoles.afterClosed()
      .subscribe(result => {
        if (result) {
          this.save(node);
        }
      });
  }


  getPublishedIcon(element: any) {
    if (this.publishedNodes) {
      let color: any = this.publishedNodes.filter(node => node.code === element.code)
        .map(node => {
            if (element.modificationDate === element.publicationDate)
              return "primary";
            else
              return "warn";
          }
        );
      if (!color || color.length <= 0)
        color = "danger";
      return color;
    }
    return "danger";
  }

  isPublished(element: any) {
    let published = this.publishedNodes.filter(node => node.code === element.code);
    return published !== null && published.length > 0;
  }

  private initPublishedNodes() {
    this.nodeService.getPublished().subscribe(
      (response: any) => {
        this.publishedNodes = response
      },
      (error) => {
        console.error('Request failed with error');
      });
  }

  private initEnvironments() {
    this.nodeService.getAllParentOrigin().subscribe(
      (response: any) => {
        this.environments = response.filter(
          (env: Node) => this.user.roles.includes("ADMIN")
            || this.user.projects.includes(env.code));
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

  setUserName(param: any) {
    this.userService.setUserName(param);
  }

  export(element: Node, environmentCode: string) {
    this.nodeService.export(element.code, environmentCode).subscribe((data: any) => {

      let blob: Blob = new Blob(['[' + data + ']'], {type: 'application/json'});

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
    return this.environments.filter((env: Node) => env.code !== this.parentNode.code);
  }
}
