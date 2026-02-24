CREATE TABLE public.currency (
    id TEXT PRIMARY KEY,
    rate BIGINT
);

CREATE TABLE public.categories (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT,
    name TEXT,
    CONSTRAINT categories_parent_id_fkey
        FOREIGN KEY (parent_id)
        REFERENCES public.categories (id)
        ON DELETE SET NULL
);

CREATE INDEX idx_categories_parent_id ON public.categories (parent_id);

CREATE TABLE public.vendor (
    code TEXT PRIMARY KEY,
    name TEXT
);

CREATE TABLE public.offers (
    id BIGINT PRIMARY KEY,
    category_id BIGINT,
    currency_id TEXT,
    vendor_code TEXT,
    available BOOLEAN,
    price BIGINT,
    count BIGINT,
    CONSTRAINT offers_category_id_fkey
        FOREIGN KEY (category_id)
        REFERENCES public.categories (id)
        ON DELETE SET NULL,
    CONSTRAINT offers_currency_id_fkey
        FOREIGN KEY (currency_id)
        REFERENCES public.currency (id)
        ON DELETE SET NULL,
    CONSTRAINT offers_vendor_code_fkey
        FOREIGN KEY (vendor_code)
        REFERENCES public.vendor (code)
        ON DELETE SET NULL
);

CREATE INDEX idx_offers_category_id ON public.offers (category_id);
CREATE INDEX idx_offers_currency_id ON public.offers (currency_id);
CREATE UNIQUE INDEX idx_offers_vendor_code ON public.offers (vendor_code);

CREATE TABLE public.offer_meta (
    id BIGSERIAL PRIMARY KEY,
    offer_id BIGINT NOT NULL UNIQUE,
    url TEXT,
    picture TEXT,
    name TEXT,
    description TEXT,
    CONSTRAINT offer_meta_offer_id_fkey
        FOREIGN KEY (offer_id)
        REFERENCES public.offers (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_offer_meta_offer_id ON public.offer_meta (offer_id);

CREATE TABLE public.offer_params (
    id BIGSERIAL PRIMARY KEY,
    offer_id BIGINT NOT NULL,
    param_name TEXT NOT NULL,
    param_value TEXT,
    CONSTRAINT offer_params_offer_id_fkey
        FOREIGN KEY (offer_id)
        REFERENCES public.offers (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_offer_params_offer_id ON public.offer_params (offer_id);
