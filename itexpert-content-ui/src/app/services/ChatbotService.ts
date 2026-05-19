// chatbot.service.ts
import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatbotConfig {
    url: string;
    apiKey: string;
    iconUrl?: string;
    welcomeMessage?: string;
    context?: string;
    language?: string;
    responseTemplate?: string;
}

export interface ChatMessage {
    role: 'user' | 'assistant' | 'system';
    content: string;
    timestamp: Date;
}

@Injectable({
    providedIn: 'root'
})
export class ChatbotService {
    private renderer: Renderer2;
    private chatbotContainer: HTMLElement | null = null;
    private chatWindow: HTMLElement | null = null;
    private isOpen = false;
    private hasBeenOpened = false;
    private messages: ChatMessage[] = [];
    private config!: ChatbotConfig;

    constructor(
        rendererFactory: RendererFactory2,
        private http: HttpClient
    ) {
        this.renderer = rendererFactory.createRenderer(null, null);
    }

    initialize(config: ChatbotConfig): void {
        this.config = config;
        this.createChatbotButton();
    }

    private createChatbotButton(): void {
        this.chatbotContainer = this.renderer.createElement('div');
        this.renderer.addClass(this.chatbotContainer, 'chatbot-container');
        this.renderer.setStyle(this.chatbotContainer, 'position', 'fixed');
        this.renderer.setStyle(this.chatbotContainer, 'bottom', '20px');
        this.renderer.setStyle(this.chatbotContainer, 'right', '20px');
        this.renderer.setStyle(this.chatbotContainer, 'z-index', '9999');

        const button = this.createButton();
        this.renderer.appendChild(this.chatbotContainer, button);
        this.renderer.appendChild(document.body, this.chatbotContainer);
    }

    private createButton(): HTMLElement {
        const button = this.renderer.createElement('button');
        this.renderer.addClass(button, 'chatbot-toggle-btn');

        this.renderer.setStyle(button, 'width', '60px');
        this.renderer.setStyle(button, 'height', '60px');
        this.renderer.setStyle(button, 'border-radius', '50%');
        this.renderer.setStyle(button, 'border', 'none');
        this.renderer.setStyle(button, 'cursor', 'pointer');
        this.renderer.setStyle(button, 'box-shadow', '0 2px 10px rgba(0,0,0,0.2)');
        this.renderer.setStyle(button, 'display', 'flex');
        this.renderer.setStyle(button, 'align-items', 'center');
        this.renderer.setStyle(button, 'justify-content', 'center');
        this.renderer.setStyle(button, 'background', 'transparent');
        this.renderer.setStyle(button, 'padding', '0');
        this.renderer.setStyle(button, 'transition', 'all 0.3s ease');

        const icon = this.renderer.createElement('img');
        this.renderer.setAttribute(icon, 'src', this.config.iconUrl || 'default-icon.png');
        this.renderer.setStyle(icon, 'width', '100%');
        this.renderer.setStyle(icon, 'height', '100%');
        this.renderer.setStyle(icon, 'border-radius', '50%');
        this.renderer.setStyle(icon, 'object-fit', 'cover');
        this.renderer.appendChild(button, icon);

        this.renderer.listen(button, 'click', () => this.toggleChatbot());

        this.renderer.listen(button, 'mouseenter', () => {
            this.renderer.setStyle(button, 'transform', 'scale(1.1)');
        });
        this.renderer.listen(button, 'mouseleave', () => {
            this.renderer.setStyle(button, 'transform', 'scale(1)');
        });

        return button;
    }

    private toggleChatbot(): void {
        if (this.isOpen) {
            this.closeChatbot();
        } else {
            this.openChatbot();
        }
    }

    private openChatbot(): void {
        if (this.chatWindow) return;

        this.chatWindow = this.createChatWindow();
        this.renderer.appendChild(document.body, this.chatWindow);

        this.renderer.setStyle(this.chatWindow, 'opacity', '0');
        this.renderer.setStyle(this.chatWindow, 'transform', 'scale(0.8)');
        setTimeout(() => {
            this.renderer.setStyle(this.chatWindow, 'opacity', '1');
            this.renderer.setStyle(this.chatWindow, 'transform', 'scale(1)');
        }, 10);

        this.isOpen = true;

        if (!this.hasBeenOpened && this.config.welcomeMessage) {
            this.hasBeenOpened = true;
            setTimeout(() => {
                this.addWelcomeMessage();
            }, 500);
        }
    }

    private createChatWindow(): HTMLElement {
        const chatWindow = this.renderer.createElement('div');
        this.renderer.addClass(chatWindow, 'chatbot-window');
        this.renderer.setStyle(chatWindow, 'position', 'fixed');
        this.renderer.setStyle(chatWindow, 'bottom', '90px');
        this.renderer.setStyle(chatWindow, 'right', '20px');
        this.renderer.setStyle(chatWindow, 'width', '380px');
        this.renderer.setStyle(chatWindow, 'height', '600px');
        this.renderer.setStyle(chatWindow, 'background', 'white');
        this.renderer.setStyle(chatWindow, 'border-radius', '12px');
        this.renderer.setStyle(chatWindow, 'box-shadow', '0 5px 20px rgba(0,0,0,0.15)');
        this.renderer.setStyle(chatWindow, 'display', 'flex');
        this.renderer.setStyle(chatWindow, 'flex-direction', 'column');
        this.renderer.setStyle(chatWindow, 'overflow', 'hidden');
        this.renderer.setStyle(chatWindow, 'transition', 'all 0.3s ease');

        const header = this.createHeader();
        this.renderer.appendChild(chatWindow, header);

        const messagesContainer = this.createMessagesContainer();
        this.renderer.appendChild(chatWindow, messagesContainer);

        const inputArea = this.createInputArea();
        this.renderer.appendChild(chatWindow, inputArea);

        return chatWindow;
    }

    private createHeader(): HTMLElement {
        const header = this.renderer.createElement('div');
        this.renderer.setStyle(header, 'padding', '15px');
        this.renderer.setStyle(header, 'background', '#007bff');
        this.renderer.setStyle(header, 'color', 'white');
        this.renderer.setStyle(header, 'font-weight', 'bold');
        this.renderer.setStyle(header, 'display', 'flex');
        this.renderer.setStyle(header, 'justify-content', 'space-between');
        this.renderer.setStyle(header, 'align-items', 'center');

        const title = this.renderer.createElement('span');
        this.renderer.setProperty(title, 'innerHTML', 'Chatbot');

        const closeBtn = this.renderer.createElement('button');
        this.renderer.setProperty(closeBtn, 'innerHTML', '✕');
        this.renderer.setStyle(closeBtn, 'background', 'transparent');
        this.renderer.setStyle(closeBtn, 'border', 'none');
        this.renderer.setStyle(closeBtn, 'color', 'white');
        this.renderer.setStyle(closeBtn, 'font-size', '20px');
        this.renderer.setStyle(closeBtn, 'cursor', 'pointer');
        this.renderer.listen(closeBtn, 'click', () => this.closeChatbot());

        this.renderer.appendChild(header, title);
        this.renderer.appendChild(header, closeBtn);

        return header;
    }

    private createMessagesContainer(): HTMLElement {
        const container = this.renderer.createElement('div');
        this.renderer.addClass(container, 'chat-messages');
        this.renderer.setStyle(container, 'flex', '1');
        this.renderer.setStyle(container, 'overflow-y', 'auto');
        this.renderer.setStyle(container, 'padding', '15px');
        this.renderer.setStyle(container, 'display', 'flex');
        this.renderer.setStyle(container, 'flex-direction', 'column');
        this.renderer.setStyle(container, 'gap', '10px');

        return container;
    }

    private createInputArea(): HTMLElement {
        const inputArea = this.renderer.createElement('div');
        this.renderer.setStyle(inputArea, 'padding', '15px');
        this.renderer.setStyle(inputArea, 'border-top', '1px solid #e0e0e0');
        this.renderer.setStyle(inputArea, 'display', 'flex');
        this.renderer.setStyle(inputArea, 'gap', '10px');

        const input = this.renderer.createElement('input');
        this.renderer.setAttribute(input, 'type', 'text');
        this.renderer.setAttribute(input, 'placeholder', 'Écrivez votre message...');
        this.renderer.setStyle(input, 'flex', '1');
        this.renderer.setStyle(input, 'padding', '10px');
        this.renderer.setStyle(input, 'border', '1px solid #ddd');
        this.renderer.setStyle(input, 'border-radius', '6px');
        this.renderer.setStyle(input, 'outline', 'none');

        const sendBtn = this.renderer.createElement('button');
        this.renderer.setProperty(sendBtn, 'innerHTML', 'Envoyer');
        this.renderer.setStyle(sendBtn, 'padding', '10px 20px');
        this.renderer.setStyle(sendBtn, 'background', '#007bff');
        this.renderer.setStyle(sendBtn, 'color', 'white');
        this.renderer.setStyle(sendBtn, 'border', 'none');
        this.renderer.setStyle(sendBtn, 'border-radius', '6px');
        this.renderer.setStyle(sendBtn, 'cursor', 'pointer');

        this.renderer.listen(sendBtn, 'click', () => {
            const message = (input as HTMLInputElement).value.trim();
            if (message) {
                this.sendMessage(message);
                (input as HTMLInputElement).value = '';
            }
        });

        this.renderer.listen(input, 'keypress', (event: KeyboardEvent) => {
            if (event.key === 'Enter') {
                const message = (input as HTMLInputElement).value.trim();
                if (message) {
                    this.sendMessage(message);
                    (input as HTMLInputElement).value = '';
                }
            }
        });

        this.renderer.appendChild(inputArea, input);
        this.renderer.appendChild(inputArea, sendBtn);

        return inputArea;
    }

    private addWelcomeMessage(): void {
        const message: ChatMessage = {
            role: 'assistant',
            content: this.config.welcomeMessage!,
            timestamp: new Date()
        };
        this.messages.push(message);
        this.displayMessage(message);
    }

    private sendMessage(userMessage: string): void {
        const userMsg: ChatMessage = {
            role: 'user',
            content: userMessage,
            timestamp: new Date()
        };
        this.messages.push(userMsg);
        this.displayMessage(userMsg);

        this.showTypingIndicator();

        this.callAI(userMessage).subscribe({
            next: (response) => {
                this.hideTypingIndicator();

                const assistantMsg: ChatMessage = {
                    role: 'assistant',
                    content: response,
                    timestamp: new Date()
                };
                this.messages.push(assistantMsg);
                this.displayMessage(assistantMsg);
            },
            error: (error) => {
                this.hideTypingIndicator();
                console.error('Erreur API:', error);

                const errorMsg: ChatMessage = {
                    role: 'assistant',
                    content: 'Désolé, une erreur est survenue. Veuillez réessayer.',
                    timestamp: new Date()
                };
                this.messages.push(errorMsg);
                this.displayMessage(errorMsg);
            }
        });
    }

    private callAI(userMessage: string): Observable<string> {
        const headers = new HttpHeaders({
            'Authorization': `Bearer ${this.config.apiKey}`,
            'Content-Type': 'application/json'
        });

        const systemPrompt = this.buildSystemPrompt();

        const body = {
            model: 'gpt-3.5-turbo',
            messages: [
                { role: 'system', content: systemPrompt },
                ...this.messages.map(m => ({ role: m.role, content: m.content }))
            ],
            temperature: 0.7
        };

        return new Observable<string>(observer => {
            this.http.post(`${this.config.url}/v1/chat/completions`, body, { headers })
                .subscribe({
                    next: (response: any) => {
                        observer.next(response.choices[0].message.content);
                        observer.complete();
                    },
                    error: (error) => {
                        observer.error(error);
                    }
                });
        });
    }

    private buildSystemPrompt(): string {
        let prompt = '';

        if (this.config.context) {
            prompt += `Context: ${this.config.context}\n\n`;
        }

        if (this.config.language) {
            prompt += `You must respond in the following language: ${this.config.language}\n\n`;
        }

        prompt += `You are a helpful and friendly assistant. 
                    You must respond with ONLY the solution in JSON format.
                    Your response must contain ONLY valid JSON, no additional text, no explanations, no markdown formatting.

                    Response template:
                    ${this.config.responseTemplate}

                    CRITICAL RULES:
                    - Return ONLY the JSON object
                    - No text before or after the JSON
                    - No code blocks (no \`\`\`json or \`\`\`)
                    - No explanations
                    - Just the raw JSON response`;

        return prompt;
    }

    private displayMessage(message: ChatMessage): void {
        const messagesContainer = document.querySelector('.chat-messages');
        if (!messagesContainer) return;

        const messageDiv = this.renderer.createElement('div');
        this.renderer.setStyle(messageDiv, 'display', 'flex');
        this.renderer.setStyle(messageDiv, 'justify-content', message.role === 'user' ? 'flex-end' : 'flex-start');

        const bubble = this.renderer.createElement('div');
        this.renderer.setStyle(bubble, 'max-width', '70%');
        this.renderer.setStyle(bubble, 'padding', '10px 15px');
        this.renderer.setStyle(bubble, 'border-radius', '12px');
        this.renderer.setStyle(bubble, 'background', message.role === 'user' ? '#007bff' : '#f1f1f1');
        this.renderer.setStyle(bubble, 'color', message.role === 'user' ? 'white' : '#333');
        this.renderer.setStyle(bubble, 'word-wrap', 'break-word');

        this.renderer.setProperty(bubble, 'innerHTML', message.content);
        this.renderer.appendChild(messageDiv, bubble);
        this.renderer.appendChild(messagesContainer, messageDiv);

        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    private showTypingIndicator(): void {
        const messagesContainer = document.querySelector('.chat-messages');
        if (!messagesContainer) return;

        const indicator = this.renderer.createElement('div');
        indicator.id = 'typing-indicator';
        this.renderer.setStyle(indicator, 'display', 'flex');
        this.renderer.setStyle(indicator, 'justify-content', 'flex-start');

        const bubble = this.renderer.createElement('div');
        this.renderer.setStyle(bubble, 'padding', '10px 15px');
        this.renderer.setStyle(bubble, 'background', '#f1f1f1');
        this.renderer.setStyle(bubble, 'border-radius', '12px');
        this.renderer.setStyle(bubble, 'color', '#333');
        this.renderer.setProperty(bubble, 'innerHTML', '...');

        this.renderer.appendChild(indicator, bubble);
        this.renderer.appendChild(messagesContainer, indicator);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    private hideTypingIndicator(): void {
        const indicator = document.getElementById('typing-indicator');
        if (indicator) {
            indicator.remove();
        }
    }

    private closeChatbot(): void {
        if (this.chatWindow) {
            this.renderer.setStyle(this.chatWindow, 'opacity', '0');
            this.renderer.setStyle(this.chatWindow, 'transform', 'scale(0.8)');

            setTimeout(() => {
                if (this.chatWindow) {
                    this.chatWindow.remove();
                    this.chatWindow = null;
                }
            }, 300);
        }
        this.isOpen = false;
    }

    destroy(): void {
        if (this.chatbotContainer) {
            this.chatbotContainer.remove();
        }
        if (this.chatWindow) {
            this.chatWindow.remove();
        }
    }
}