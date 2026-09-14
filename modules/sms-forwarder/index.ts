import { requireOptionalNativeModule } from 'expo';

export type Config = { id: string; name: string; enabled: boolean; senders: string[]; recipients: string[]; removals: string[]; removalLabels?: string[] };
export type HistoryEntry = {
  id: string; configId?: string; configName?: string; sender: string; recipient: string; time: number;
  status: 'pending' | 'sent' | 'failed'; detail: string; message?: string;
};
export type SmsPreview = { sender: string; body: string; time: number };
export type State = { configs: Config[]; history: HistoryEntry[] };

export default requireOptionalNativeModule<{
  getState(): Promise<string>;
  saveConfigs(json: string): Promise<string>;
  getRecentMessages(): Promise<string>;
  clearHistory(): Promise<void>;
}>('SmsForwarder');
