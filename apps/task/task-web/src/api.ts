import {authorizedRequest as request} from './auth'
export type InputOption={label:string;value:string}
export type InputField={key:string;label:string;type:'TEXT'|'TEXTAREA'|'PATH'|'PASSWORD'|'SELECT'|'JSON_OBJECT'|'JSON_ARRAY'|'IMAGE_FILE'|'URL';required:boolean;placeholder:string;description:string;defaultValue:string;options:InputOption[];secret:boolean}
export type Task={id:string;displayName:string;description:string;category:string;merchantId?:string;merchantName?:string;templateTaskId:string;workflowTemplateId?:string;workflowName?:string;className:string;enabled:boolean;dangerous:boolean;inputFields:InputField[]}
export type TaskTemplate={id:string;displayName:string;description:string;category:string;className:string;dangerous:boolean;inputFields:InputField[]}
export type WorkflowStep={key:string;order:number;templateTaskId:string;templateName:string;optional:boolean;polling:boolean;intervalSeconds:number;maxAttempts:number}
export type WorkflowTemplate={id:string;displayName:string;description:string;category:string;version:number;dangerous:boolean;steps:WorkflowStep[];inputFields:InputField[]}
export type Merchant={id:string;code:string;name:string;description:string;configurationKeys:string[];updatedAt:string}
export type MerchantConfigurationField={key:string;label:string;description:string;placeholder:string;required:boolean;secret:boolean;multiline:boolean}
export type StepExecution={id:string;stepIndex:number;stepKey:string;templateTaskId:string;status:string;attemptCount:number;outputs:Record<string,unknown>;startedAt?:string;finishedAt?:string;nextRunAt?:string;errorMessage?:string}
export type Execution={id:string;taskId:string;taskName:string;status:string;arguments:string[];inputs:Record<string,string>;triggerSource:string;executionType:string;confirmed:boolean;requestedAt:string;startedAt?:string;finishedAt?:string;nextRunAt?:string;errorMessage?:string;outputs:Record<string,unknown>;steps:StepExecution[]}
export type ExecutionLog={id:number;level:string;message:string;createdAt:string}
export const api={
  tasks:()=>request<Task[]>('/api/v1/tasks'),
  templates:()=>request<TaskTemplate[]>('/api/v1/tasks/templates'),
  workflows:()=>request<WorkflowTemplate[]>('/api/v1/tasks/workflows'),
  createTask:(value:{id:string;displayName:string;description?:string;merchantId:string;templateTaskId?:string;workflowTemplateId?:string})=>request<Task>('/api/v1/tasks',{method:'POST',body:JSON.stringify(value)}),
  merchants:()=>request<Merchant[]>('/api/v1/merchants'),
  merchantConfigurationFields:()=>request<MerchantConfigurationField[]>('/api/v1/merchants/configuration-fields'),
  createMerchant:(value:{code:string;name:string;description?:string;configuration:Record<string,string>})=>request<Merchant>('/api/v1/merchants',{method:'POST',body:JSON.stringify(value)}),
  merchantConfiguration:(id:string)=>request<{merchantId:string;configuration:Record<string,string>}>(`/api/v1/merchants/${id}/configuration`),
  updateMerchantConfiguration:(id:string,configuration:Record<string,string>)=>request<{merchantId:string;configuration:Record<string,string>}>(`/api/v1/merchants/${id}/configuration`,{method:'PUT',body:JSON.stringify({configuration})}),
  enable:(id:string,enabled:boolean)=>request<Task>(`/api/v1/tasks/${id}/enabled`,{method:'PUT',body:JSON.stringify({enabled})}),
  run:(id:string,arguments_:string[],inputs:Record<string,string>,confirmed:boolean)=>request<Execution>(`/api/v1/tasks/${id}/executions`,{method:'POST',body:JSON.stringify({arguments:arguments_,inputs,confirmed})}),
  executions:()=>request<Execution[]>('/api/v1/executions'),
  logs:(id:string)=>request<ExecutionLog[]>(`/api/v1/executions/${id}/logs`),
  queryNow:(id:string)=>request<Execution>(`/api/v1/executions/${id}/query-now`,{method:'POST'}),
  uploadImage:(file:File)=>{const body=new FormData();body.append('file',file);return request<{path:string;name:string}>('/api/v1/uploads/images',{method:'POST',body})},
}
