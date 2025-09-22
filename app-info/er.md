
## ER図
```mermaid
erDiagram
  EXTERNAL_FIREBASE_USER {
    uuid id PK "ユーザーID(主キー)"
  }

  CATEGORY {
    uuid id PK "カテゴリID(主キー)"
    uuid user_id FK "ユーザーID(外部キー)"
    string name "カテゴリ名"
    boolean deleted_flag "削除フラグ"
  }

  ITEM {
    uuid id PK "アイテムID(主キー)"
    uuid category_id FK "カテゴリID(外部キー)"
    uuid user_id FK "ユーザーID(外部キー)"
    string name "アイテム名"
    boolean deleted_flag "削除フラグ"
    datetime updated_at "更新日時"
  }

  ITEM_RECORD {
    uuid id PK "履歴ID(主キー)"
    uuid user_id FK "ユーザーID(外部キー)"
    uuid item_id FK "アイテムID(外部キー)"
    int quantity "数量"
    int price "単価"
    datetime expiration_date "有効期限"
    datetime updated_at "更新日時"
      enum source "入出庫区分(IN/OUT)"
      uuid item_record_id FK "関連履歴ID(外部キー,IN履歴を参照,削除時OUTも削除)"
  }

  EXTERNAL_FIREBASE_USER ||--o{ CATEGORY : "作成"
  EXTERNAL_FIREBASE_USER ||--o{ ITEM : "作成"
  EXTERNAL_FIREBASE_USER ||--o{ ITEM_RECORD : "作成"
  CATEGORY ||--o{ ITEM : "カテゴリ"
  ITEM ||--o{ ITEM_RECORD : "履歴"
```


## DBML
```
Table EXTERNAL_FIREBASE_USER {
  id uuid [pk]
}

Table category {
  id uuid [pk]
  user_id uuid [ref: > EXTERNAL_FIREBASE_USER.id,not null]
  name string [note: 'カテゴリ名', not null]
  deleted_flag boolean [note: '削除フラグ', default: false]
}

Table item {
  id uuid [pk]
  category_id uuid [ref: > category.id,not null]
  user_id uuid [ref: > EXTERNAL_FIREBASE_USER.id, not null]
  name string [note: 'アイテム名', not null]
  deleted_flag boolean [note: '削除フラグ', default: false]
  updated_at datetime [note: '更新日時', default: `current_timestamp`]
}

Table item_record {
  id uuid [pk]
  user_id uuid [ref: > EXTERNAL_FIREBASE_USER.id, not null]
  item_id uuid [ref: > item.id, not null]
  quantity int [note: '数量', default: 0]
  price int [note: '単価', default: 0]
  expiration_date datetime [note: '有効期限', default: null]
  updated_at datetime [note:"更新日時",default: `current_timestamp`]
  source enum('IN', 'OUT') [note: '入出庫区分', not null]
  item_record_id uuid [ref: > item_record.id, note: '関連履歴ID', default: null]
}
```
