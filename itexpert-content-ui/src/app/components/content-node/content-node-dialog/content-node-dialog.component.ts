import { Component, Inject, OnDestroy, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { Node } from "../../../modeles/Node";
import { TranslateService } from "@ngx-translate/core";
import { ToastrService } from "ngx-toastr";
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from "@angular/material/dialog";
import { ContentNodeService } from "../../../services/ContentNodeService";
import { ContentNode } from "../../../modeles/ContentNode";
import { ValidationDialogComponent } from "../../commons/validation-dialog/validation-dialog.component";
import { MatTableDataSource } from "@angular/material/table";
import { User } from "../../../modeles/User";
import { LoggerService } from "../../../services/LoggerService";
import { ContentUrl } from "../../../modeles/ContentUrl";
import { NodeRulesConditionsDialogComponent } from "../../commons/node-rules-conditions-dialog/node-rules-conditions-dialog.component";
import { ValuesDialogComponent } from "../../commons/values-dialog/values-dialog.component";
import { StatusEnum } from "../../../modeles/StatusEnum";
import { PublishedItemsDialogComponent } from "../../commons/published-items-dialog/published-items-dialog.component";
import { TranslationsDialogComponent } from "../../commons/translations-dialog/translations-dialog.component";
import { UserService } from "../../../services/UserService";
import { UserAccessService } from "../../../services/UserAccessService";
import { Env } from "../../../../assets/configurations/environment";
import { DeletedItemsDialogComponent } from "../../commons/deleted-items-dialog/deleted-items-dialog.component";
import { NodeService } from "../../../services/NodeService";
import { ContentDatasComponent } from "../content-datas/content-datas.component";
import { DataService } from "../../../services/DataService";
import { ContentCodeComponent } from "../content-code/content-code.component";
import { LockService } from 'src/app/services/LockService';
import { interval, Subscription, of } from 'rxjs';
import { AuthenticationService } from "../../../services/AuthenticationService";
import { catchError, finalize, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-node-content-dialog',
  templateUrl: './content-node-dialog.component.html',
  styleUrls: ['./content-node-dialog.component.css'],
  standalone: false
})
export class ContentNodeDialogComponent implements OnInit, OnDestroy {
  user: WritableSignal<User> = signal<User>({} as User);
  node: Node;
  type: string;
  currentContent: ContentNode;
  environments: WritableSignal<Node[]> = signal<Node[]>([]);
  mapDatas: WritableSignal<Map<string, boolean>> = signal<Map<string, boolean>>(new Map());
  displayedColumns: string[] = ['Status', 'Type', 'Version', 'Last Modification', 'Modified by', 'Translations', 'Rules', 'Values', 'Publication', 'Actions'];
  dataSource: WritableSignal<MatTableDataSource<ContentNode>> = signal(new MatTableDataSource<ContentNode>([]));
  totalDeleteds: WritableSignal<number> = signal(0);
  isLoading: WritableSignal<boolean> = signal(false);

  dialogRefPublish: MatDialogRef<ValidationDialogComponent>;
  dialogRefValues: MatDialogRef<ValuesDialogComponent>;
  dialogRefRules: MatDialogRef<NodeRulesConditionsDialogComponent>;
  dialogRefDeleteds: MatDialogRef<DeletedItemsDialogComponent>;
  dialogRefPublished: MatDialogRef<PublishedItemsDialogComponent>;
  dialogRefTranslations: MatDialogRef<TranslationsDialogComponent>;
  dialogRefDelete: MatDialogRef<ValidationDialogComponent>;
  dialogRefDatas: MatDialogRef<ContentDatasComponent>;
  dialogRefCode: MatDialogRef<ContentCodeComponent>;

  private lockRefreshSub?: Subscription;
  private subscriptions: Subscription[] = [];

  private translate = inject(TranslateService);
  private toast = inject(ToastrService);
  private loggerService = inject(LoggerService);
  private contentNodeService = inject(ContentNodeService);
  private dataService = inject(DataService);
  public userAccessService = inject(UserAccessService);
  public nodeService = inject(NodeService);
  public dialogRef = inject(MatDialogRef<ContentNodeDialogComponent>);
  private userService = inject(UserService);
  private lockService = inject(LockService);
  private authenticationService = inject(AuthenticationService);
  private dialog = inject(MatDialog);
  private content = inject(MAT_DIALOG_DATA);

  constructor() {
    if (this.content) {
      this.node = this.content;
    }
  }

  ngOnInit() {
    this.user.set(this.userAccessService.getCurrentUser());
    this.init();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    if (this.lockRefreshSub) {
      this.lockRefreshSub.unsubscribe();
    }
  }

  private initLocks(contents: ContentNode[]) {
    contents.forEach((content: ContentNode) => {
      const lockSub = this.lockService.getLockInfoSocket(content.code, this.authenticationService.getAccessToken()).subscribe((lockInfo: any) => {
        content.lockInfo = lockInfo;
      });
      this.subscriptions.push(lockSub);
    });
  }

  init() {
    this.isLoading.set(true);
    this.currentContent = null as any;
    this.mapDatas.set(new Map());

    const contentSub = this.contentNodeService.getAllByParentCodeAndStatus(this.node.code, StatusEnum.SNAPSHOT).pipe(
      catchError((error) => {
        this.toast.error('Request failed with error');
        return of([]);
      }),
      finalize(() => this.isLoading.set(false))
    ).subscribe((response: any) => {
      this.fetchDatas(response);
      this.initLocks(response);

      response = response.sort((a: any, b: any) => {
        if (a.favorite && !b.favorite) return -1;
        if (!a.favorite && b.favorite) return 1;
        return a.code.localeCompare(b.code);
      });

      this.dataSource.set(new MatTableDataSource(response));
      this.initEnvironments();
    });
    this.subscriptions.push(contentSub);

    const deletedSub = this.contentNodeService.getDeleted(this.node?.code ?? '').pipe(
      catchError((error: any) => {
        console.error(error);
        return of([]);
      })
    ).subscribe((response: any) => {
      this.totalDeleteds.set(response.length);
    });
    this.subscriptions.push(deletedSub);
  }

  private initEnvironments() {
    const envSub = this.nodeService.getAllParentOrigin().pipe(
      catchError((error) => {
        console.error('Request failed with error');
        return of([]);
      })
    ).subscribe((response: any) => {
      this.environments.set(response.filter((env: Node) =>
        this.user().roles.includes("ADMIN") || this.user().projects.includes(env.code)
      ));
    });
    this.subscriptions.push(envSub);
  }

  validate() {
    this.save(this.currentContent);
  }

  create() {
    if (this.currentContent) {
      this.dialogRefPublish = this.dialog.open(ValidationDialogComponent, {
        data: {
          title: "CANCEL_MODIFICATIONS_TITLE",
          message: "CANCEL_MODIFICATIONS_MESSAGE"
        },
        height: '80vh',
        width: '80vw',
        disableClose: true
      });
      const dialogSub = this.dialogRefPublish.afterClosed()
        .subscribe(result => {
          if (result?.data === 'validated') {
            this.createNewContent();
          }
        });
      this.subscriptions.push(dialogSub);
    } else {
      this.createNewContent();
    }
  }

  publish(content: ContentNode) {
    this.dialogRefPublish = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "PUBLISH_PROJECT_TITLE",
        message: "PUBLISH_PROJECT_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefPublish.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          const publishSub = this.contentNodeService.publish(content.code, true).pipe(
            switchMap(() => this.translate.get("SAVE_SUCCESS")),
            catchError((error) => {
              return this.translate.get("SAVE_ERROR").pipe(
                switchMap((trad1: string) => {
                  return this.translate.get("CHANGE_PROJECT_CODE_MESSAGE").pipe(
                    switchMap((trad2: string) => {
                      this.loggerService.error(trad1 + ",  " + trad2);
                      throw error;
                    })
                  );
                })
              );
            })
          ).subscribe((trad: string) => {
            this.loggerService.success(trad);
            this.init();
          });
          this.subscriptions.push(publishSub);
        }
      });
    this.subscriptions.push(dialogSub);
  }

  delete(node: Node) {
    this.dialogRefDelete = this.dialog.open(ValidationDialogComponent, {
      data: {
        title: "DELETE_CONTENT_NODE_TITLE",
        message: "DELETE_CONTENT_NODE_MESSAGE"
      },
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefDelete.afterClosed()
      .subscribe(result => {
        if (result && result.data !== 'canceled') {
          const deleteSub = this.contentNodeService.delete(node.code).pipe(
            switchMap(() => this.translate.get("DELETE_SUCCESS")),
            catchError((error) => {
              return this.translate.get("DELETE_ERROR").pipe(
                switchMap((trad: string) => {
                  this.loggerService.error(trad);
                  throw error;
                })
              );
            })
          ).subscribe((trad: string) => {
            this.loggerService.success(trad);
            this.init();
          });
          this.subscriptions.push(deleteSub);
        }
      });
    this.subscriptions.push(dialogSub);
  }

  update(content: ContentNode) {
    this.dialogRefCode = this.dialog.open(ContentCodeComponent, {
      data: {
        node: this.node,
        contentNode: content,
        type: this.type,
        user: this.user()
      },
      height: '100%',
      width: '90vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefCode.afterClosed()
      .subscribe((data: any) => {
        if (data?.refresh) {
          this.init();
        }
      });
    this.subscriptions.push(dialogSub);
  }

  private createNewContent() {
    this.currentContent = new ContentNode();
    this.currentContent.type = this.type;
    this.currentContent.snapshot = true;
    this.currentContent.parentCode = this.node.code;
    if (this.currentContent && !this.currentContent.id) {
      this.currentContent.code = this.type.replace(/[\W_]+/g, "_").toUpperCase() + '-'
        + (this.node.parentCode ? this.node.parentCode.split("-")[0] + '-' : this.node.code.split("-")[0] + '-')
        + (new Date()).getTime();
    }
    this.update(this.currentContent);
  }

  addUrl() {
    if (this.type === 'URLS') {
      if (!this.currentContent.urls)
        this.currentContent.urls = [];

      let url: ContentUrl = new ContentUrl();
      url.url = "";
      url.type = "";
      url.description = "";
      this.currentContent.urls.push(url);
    }
  }

  rules(content: ContentNode) {
    this.dialogRefRules = this.dialog.open(NodeRulesConditionsDialogComponent, {
      data: content,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefRules.afterClosed()
      .subscribe(result => {
        this.save(result.data);
      });
    this.subscriptions.push(dialogSub);
  }

  values(content: ContentNode) {
    this.dialogRefValues = this.dialog.open(ValuesDialogComponent, {
      data: content,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefValues.afterClosed()
      .subscribe(result => {
        this.save(result.data);
      });
    this.subscriptions.push(dialogSub);
  }

  private save(content: ContentNode) {
    this.isLoading.set(true);
    content.modifiedBy = this.user().id;
    content.parentCode = this.node.code;
    content.parentCodeOrigin = this.node.parentCodeOrigin;

    const saveSub = this.contentNodeService.save(content).pipe(
      switchMap((response: any) => {
        this.currentContent = response;
        return this.translate.get("SAVE_SUCCESS");
      }),
      catchError((error) => {
        return this.translate.get("SAVE_ERROR").pipe(
          switchMap((trad1: string) => {
            return this.translate.get("CHANGE_PROJECT_CODE_MESSAGE").pipe(
              switchMap((trad2: string) => {
                this.loggerService.error(trad1 + ",  " + trad2);
                throw error;
              })
            );
          })
        );
      }),
      finalize(() => this.isLoading.set(false))
    ).subscribe((trad: string) => {
      this.loggerService.success(trad);
      this.init();
    });
    this.subscriptions.push(saveSub);
  }

  getPublishedIcon(element: any) {
    if (element.publicationStatus === 'PUBLISHED')
      return "green";
    else if (element.publicationStatus === 'SNAPSHOT')
      return "yellow";
    else
      return "red";
  }

  gotoPublished(content: ContentNode) {
    this.dialogRefPublished = this.dialog.open(PublishedItemsDialogComponent, {
      height: '80vh',
      width: '80vw',
      disableClose: true,
      data: {
        itemName: content.type,
        itemCode: content.code,
        itemIcon: 'perm_media',
        titleKey: 'CONTENT_PUBLICATION_HISTORY',
        displayTypeColumn: false,
        publicationService: this.contentNodeService
      }
    });
    const dialogSub = this.dialogRefPublished.afterClosed().subscribe(() => {
      this.init();
    });
    this.subscriptions.push(dialogSub);
  }

  translations(contentNode: ContentNode) {
    this.dialogRefTranslations = this.dialog.open(TranslationsDialogComponent, {
      data: contentNode,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefTranslations.afterClosed()
      .subscribe(result => {
        if (result) {
          this.save(contentNode);
        }
      });
    this.subscriptions.push(dialogSub);
  }

  deleteUrl(url: ContentUrl) {
    if (url) {
      this.currentContent.urls = this.currentContent.urls.filter((v: ContentUrl) =>
        (v.url !== url.url && v.type !== url.type && v.description !== url.description)
      );
    }
  }

  export(element: ContentNode, environmentCode: string) {
    const exportSub = this.contentNodeService.export(element.code, environmentCode).subscribe((data: any) => {
      let blob: Blob = new Blob([JSON.stringify(data)], { type: 'application/json' });
      const a = document.createElement('a');
      const objectUrl = URL.createObjectURL(blob);
      a.href = objectUrl;
      let fileName: string;
      fileName = element.code + (environmentCode && environmentCode.length > 0 ? "_" + environmentCode : "") + ".json";
      a.download = fileName;
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
    const reader: any = new FileReader();
    reader.readAsText(file, "UTF-8");
    let callback = this;
    reader.onload = ((evt: any) => {
      const content = evt.target.result;
      const importSub = callback.contentNodeService.import(callback.node.code, JSON.parse(content)).subscribe((data: any) => {
        callback.translate.get("IMPORT_SUCCESS").subscribe((translation: string) => {
          callback.toast.success(translation);
          callback.init();
        });
      });
      callback.subscriptions.push(importSub);
    }).bind(this);
  }

  view(element: ContentNode) {
    window.open(Env.EXPERT_CONTENT_API_URL + "/contents/code/"
      + element.code
      + ((element.type && (element.type === 'PICTURE' || element.type === 'FILE')) ? '/file?status=' + StatusEnum.SNAPSHOT : '?payloadOnly=true&status=' + StatusEnum.SNAPSHOT), element.code);
  }

  deleteds() {
    this.dialogRefDeleteds = this.dialog.open(DeletedItemsDialogComponent, {
      height: '80vh',
      width: '80vw',
      disableClose: true,
      data: {
        parentNode: this.node,
        titleKey: 'DELETED_CONTENTS',
        icon: 'delete',
        displayTypeColumn: false,
        deleteService: this.contentNodeService
      }
    });
    const dialogSub = this.dialogRefDeleteds.afterClosed()
      .subscribe(() => {
        this.init();
      });
    this.subscriptions.push(dialogSub);
  }

  deploy(element: any, environmentCode: string) {
    const deploySub = this.contentNodeService.deploy(element.code, environmentCode).subscribe((data: any) => {
      this.translate.get("DEPLOY_SUCCESS").subscribe((translation: string) => {
        this.toast.success(translation);
      });
    });
    this.subscriptions.push(deploySub);
  }

  getEnvironments() {
    try {
      return this.environments().filter((env: Node) =>
        this.node.parentCodeOrigin ? env.code !== this.node.parentCodeOrigin : env.code !== this.node.code
      );
    } catch (error: any) {
      return null;
    }
  }

  close() {
    this.currentContent = null as any;
  }

  datas(contentNode: ContentNode) {
    this.dialogRefDatas = this.dialog.open(ContentDatasComponent, {
      data: contentNode,
      height: '80vh',
      width: '80vw',
      disableClose: true
    });
    const dialogSub = this.dialogRefDatas.afterClosed()
      .subscribe(() => {
        this.init();
      });
    this.subscriptions.push(dialogSub);
  }

  fetchDatas(contents: any) {
    this.fetchLocksFactory(contents);
    this.lockRefreshSub = interval(10000).subscribe(() => {
      this.fetchLocksFactory(contents);
    });
  }

  fetchLocksFactory(contents: any[]) {
    contents.forEach((content: any) => {
      const dataSub = this.dataService
        .countDatasByContentNodeCodeWebSocket(content.code)
        .subscribe({
          next: (count: number) => {
            const currentMap = this.mapDatas();
            currentMap.set(content.code, count > 0);
            this.mapDatas.set(currentMap);
          },
          error: (err) => {
            console.error('WebSocket error', err);
            const currentMap = this.mapDatas();
            currentMap.set(content.code, false);
            this.mapDatas.set(currentMap);
          }
        });
      this.subscriptions.push(dataSub);
    });
  }

  haveDatas(code: string): boolean {
    return this.mapDatas().get(code) ?? false;
  }

  favorite(element: ContentNode) {
    element.favorite = !element.favorite;
    this.save(element);
  }
}