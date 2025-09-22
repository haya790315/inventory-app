## アイテム入出庫履歴 CRUD API シーケンス図

```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant API
    participant Service
    participant DB

    %% アイテム履歴作成（入庫/出庫）
    User->>Frontend: 入庫/出庫リクエスト送信
    Frontend->>API: POST /api/item/records (JSON)
    API->>Service: バリデーション・処理依頼
    Service->>DB: 履歴レコード作成/在庫チェック
    DB-->>Service: 作成結果/在庫情報
    Service->>API: 成功/エラー結果
    API->>Frontend: レスポンス (成功/エラー)
    Frontend->>User: メッセージ表示

    %% 履歴リスト取得
    User->>Frontend: 履歴一覧表示要求
    Frontend->>API: GET /api/item/records
    API->>Service: 履歴一覧取得
    Service->>DB: 履歴レコード検索
    DB-->>Service: 履歴リスト
    Service->>API: 履歴リスト返却
    API->>Frontend: レスポンス (履歴リスト)
    Frontend->>User: 履歴表示

    %% 単体履歴取得
    User->>Frontend: 履歴詳細表示要求
    Frontend->>API: GET /api/item/records?record_id={record_id}
    API->>Service: 履歴詳細取得
    Service->>DB: 履歴レコード検索
    DB-->>Service: 履歴詳細
    Service->>API: 履歴詳細返却
    API->>Frontend: レスポンス (履歴詳細)
    Frontend->>User: 履歴詳細表示

    %% 履歴削除
    User->>Frontend: 履歴削除要求
    Frontend->>API: DELETE /api/item/records?record_id={record_id}
    API->>Service: 履歴削除依頼
    Service->>DB: 履歴レコード削除
    DB-->>Service: 削除結果
    Service->>API: 削除結果返却
    API->>Frontend: レスポンス (削除結果)
    Frontend->>User: メッセージ表示

    %% 指定アイテムの全履歴取得
    User->>Frontend: 指定アイテム履歴表示要求
    Frontend->>API: GET /api/items/{item_id}/records
    API->>Service: 指定アイテム履歴取得
    Service->>DB: 指定アイテム履歴検索
    DB-->>Service: 履歴リスト
    Service->>API: 履歴リスト返却
    API->>Frontend: レスポンス (履歴リスト)
    Frontend->>User: 履歴表示
